package com.minecraftloveyou.module.reroll;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.core.WlyNet;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 刷新附魔（reroll）：潜行+右键附魔台 → 消耗经验重掷三个选项。
 * 实现服务端：改玩家附魔种子 + 强制 EnchantmentMenu 重算。
 *
 * <p>客户端按键逻辑在 {@code client/RerollKeyHandler}（本项目铁律 8：
 * 本类由通用入口注册，不得引用 {@code net.minecraft.client.*}）。
 */
public class RerollModule implements WlyModule {

    public static final String ID = "reroll";
    /** 重掷消耗的经验等级。 */
    public static int rerollCostLevels = 3;

    public static void register() {
        ModuleManager.register(new RerollModule());
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
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    /** C2S 重掷请求（空载荷）。 */
    public static final class RerollRequest implements CustomPacketPayload {
        public static final RerollRequest INSTANCE = new RerollRequest();
        public static final Type<RerollRequest> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft_love_you", "reroll"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RerollRequest> CODEC =
                net.minecraft.network.codec.StreamCodec.of((b, p) -> {}, b -> INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!on() || world.isClientSide() || player.isSpectator() || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(world.getBlockState(pos).getBlock() instanceof EnchantingTableBlock)) {
            return InteractionResult.PASS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        // 关键：只有「确实处于附魔界面」才算这次交互被我们吃掉了。
        // 早期实现无条件 return SUCCESS —— 潜行右键附魔台（尚未打开界面）时
        // 交互被吞、原版界面永远打不开，玩家表现为「附魔台点不动」。
        if (!(sp.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu)) {
            return InteractionResult.PASS;
        }
        handleReroll(sp);
        world.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** 服务端：执行重掷（按键/潜行右键共用）。 */
    public static void handleReroll(ServerPlayer sp) {
        if (!WlyModule.on(ID)) {
            return;
        }
        if (!(sp.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu menu)) {
            return;
        }
        if (!sp.hasInfiniteMaterials()) {
            if (sp.experienceLevel < rerollCostLevels) {
                sp.sendSystemMessage(com.minecraftloveyou.core.WlyCommands.prefix()
                        .append(Component.translatable("wly.msg.reroll.no_xp", rerollCostLevels)), true);
                return;
            }
            sp.giveExperienceLevels(-rerollCostLevels);
        }
        int newSeed = sp.getRandom().nextInt();
        ((com.minecraftloveyou.mixin.PlayerSeedAccessor) (Object) sp).minecraftloveyou$setEnchantmentSeed(newSeed);
        {
            com.minecraftloveyou.core.WlyNetBridge.rerollEnchantmentMenu(sp, menu, newSeed);
        }
        sp.sendSystemMessage(com.minecraftloveyou.core.WlyCommands.prefix()
                .append(Component.translatable("wly.msg.reroll.done")), true);
    }
}