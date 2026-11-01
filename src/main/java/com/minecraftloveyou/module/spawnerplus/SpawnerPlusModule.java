package com.minecraftloveyou.module.spawnerplus;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 刷怪笼增强（spawner-plus）：
 * ① 手持刷怪蛋右键刷怪笼 → 改变刷怪类型（消耗刷怪蛋）
 * ② 破坏刷怪笼 → 掉落刷怪笼本体（任意镐）
 * ③（击杀掉蛋：依赖死亡来源追踪，后续版本）
 */
public class SpawnerPlusModule implements WlyModule {

    public static final String ID = "spawner-plus";
    /** 破坏刷怪笼掉落本体。 */
    public static boolean dropSpawnerOnBreak = true;

    public static void register() {
        ModuleManager.register(new SpawnerPlusModule());
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
        // ① 刷怪蛋改类型
        UseBlockCallback.EVENT.register(this::onUseBlock);
        // ② 破坏掉笼
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!on() || !dropSpawnerOnBreak || world.isClientSide()) {
                return;
            }
            if (state.getBlock() instanceof net.minecraft.world.level.block.SpawnerBlock) {
                // 只掉一个本体：早期还额外调了一次 dropResources，如果数据包给刷怪笼补了
                // 掉落表，就会掉出两个刷怪笼。这里统一只 popResource。
                net.minecraft.world.level.block.Block.popResource(world, pos,
                        new ItemStack(net.minecraft.world.level.block.Blocks.SPAWNER.asItem()));
            }
        });
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!on() || world.isClientSide() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof SpawnEggItem egg)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(world.getBlockEntity(pos) instanceof SpawnerBlockEntity spawnerBE)) {
            return InteractionResult.PASS;
        }
        EntityType<?> type = egg.getType(held);
        // 26.2 BaseSpawner.setEntityId(type, level, random, pos)
        spawnerBE.getSpawner().setEntityId(type, world, player.getRandom(), pos);
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ((net.minecraft.server.level.ServerPlayer) player).sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "[我的世界爱你] 刷怪笼类型已改为 " + type.toShortString()), true);
        return InteractionResult.SUCCESS;
    }
}