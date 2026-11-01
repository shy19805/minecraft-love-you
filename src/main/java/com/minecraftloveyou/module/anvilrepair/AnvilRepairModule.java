package com.minecraftloveyou.module.anvilrepair;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyCommands;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铁砧修复（anvil-repair）：手持铁锭**左键**铁砧 → 消耗若干铁锭，把铁砧升一档。
 *
 * <p>三档状态（{@code AnvilBlock} 的三个方块，属性集完全相同，只有 {@code FACING}）：
 * <pre>
 *   damaged_anvil（损坏）  →  chipped_anvil（开裂）  →  anvil（完好）
 * </pre>
 * 每次左键只升**一档**；完好铁砧不再消耗，交回原版处理（正常挖掉）。
 *
 * <p><b>为什么用左键而不是按键</b>：按键方案（{@code WlyKeys}）在容器界面/连续触发/
 * 改键冲突下都容易「按了没反应」，玩家反馈无法正常使用。左键铁砧是原版就有的
 * 交互动作，配合 {@link AttackBlockCallback} 天然可用、可取消、无需额外键位。
 *
 * <p><b>实现要点</b>：
 * <ol>
 *   <li>{@code AttackBlockCallback} 返回 {@code SUCCESS} 即取消原版的方块破坏
 *       （Fabric 在 {@code ServerPlayerGameMode.startBlockBreak} 上注入，取消时会补发
 *       {@code ClientboundBlockUpdatePacket} 让客户端撤销破坏动画）。</li>
 *   <li>只在**服务端**改世界；客户端一律 PASS（与 harvest / reroll / beacon-fill 同一口径，
 *       避免专服与客户端配置不一致时出现「点了没反应」）。</li>
 *   <li><b>「手持铁锭左键铁砧」这个动作永远不会把铁砧挖掉</b> —— 铁锭够就修复、不够就提示、
 *       铁砧已完好就什么都不做，三种情况都返回 {@code SUCCESS} 取消破坏。
 *       只有**没拿铁锭**时才交回原版（否则玩家就没法正常挖掉铁砧了）。</li>
 *   <li>创造模式不扣铁锭（参见避坑手册 #67：每一条消耗路径都要单独问一句创造模式）。</li>
 * </ol>
 */
public class AnvilRepairModule implements WlyModule {

    public static final String ID = "anvil-repair";

    /** 每修复一档消耗的铁锭数（配置项，1~31）。 */
    public static int repairCost = 3;

    public static void register() {
        ModuleManager.register(new AnvilRepairModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "wly.module." + ID;
    }

    @Override
    public void init(ModuleContext ctx) {
        AttackBlockCallback.EVENT.register(this::onAttackBlock);
    }

    /** 左键方块。返回值非 PASS 即取消原版的方块破坏。 */
    private InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand,
                                            BlockPos pos, Direction direction) {
        if (!on() || player.isSpectator() || world.isClientSide()) {
            return InteractionResult.PASS;
        }
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof AnvilBlock)) {
            return InteractionResult.PASS;
        }
        ItemStack ingots = ingotsInHand(player);
        if (ingots.isEmpty()) {
            return InteractionResult.PASS;   // 手上没铁锭 → 交回原版，正常挖
        }
        // 从这一行往下：玩家**表达了修复意图**（拿着铁锭左键铁砧），
        // 因此绝不允许原版把铁砧挖掉 —— 只可能「修复」或「什么都不做」。
        BlockState repaired = repairedState(state);
        if (repaired == null) {
            return InteractionResult.SUCCESS; // 已是完好铁砧 → 无反应（也不挖掉）
        }
        boolean creative = player.getAbilities().instabuild;
        int cost = Math.max(1, repairCost);
        if (!creative && ingots.getCount() < cost) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(WlyCommands.prefix()
                        .append(Component.translatable("wly.msg.anvil_repair.not_enough", cost)), true);
            }
            return InteractionResult.SUCCESS; // 铁锭不够 → 只提示，不挖掉
        }
        if (!(world instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        level.setBlockAndUpdate(pos, repaired);
        if (!creative) {
            ingots.shrink(cost);
        }
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.9F, 1.0F);
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(WlyCommands.prefix()
                    .append(Component.translatable("wly.msg.anvil_repair.done", cost)), true);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 返回「升一档」后的方块状态；已是完好铁砧返回 {@code null}。
     * 只保留 {@code FACING}（三个铁砧方块属性集相同，其余属性不存在）。
     * <p>public 供自检 T16 直接断言映射表（纯函数，无副作用）。
     */
    public static BlockState repairedState(BlockState state) {
        Block source = state.getBlock();
        Block target;
        if (source == Blocks.DAMAGED_ANVIL) {
            target = Blocks.CHIPPED_ANVIL;
        } else if (source == Blocks.CHIPPED_ANVIL) {
            target = Blocks.ANVIL;
        } else {
            return null;
        }
        BlockState out = target.defaultBlockState();
        if (out.hasProperty(AnvilBlock.FACING) && state.hasProperty(AnvilBlock.FACING)) {
            out = out.setValue(AnvilBlock.FACING, state.getValue(AnvilBlock.FACING));
        }
        return out;
    }

    /** 主手优先，其次副手；都没有铁锭返回空栈。 */
    private static ItemStack ingotsInHand(Player player) {
        Item ingot = Items.IRON_INGOT;
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() == ingot) {
            return main;
        }
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() == ingot) {
            return off;
        }
        return ItemStack.EMPTY;
    }
}
