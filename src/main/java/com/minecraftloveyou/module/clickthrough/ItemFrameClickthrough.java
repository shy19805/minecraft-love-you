package com.minecraftloveyou.module.clickthrough;

import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 物品展示框（含发光）右键 → 穿透到后方方块。 */
public final class ItemFrameClickthrough {

    private ItemFrameClickthrough() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(ItemFrameClickthrough::onUseEntity);
    }

    private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand,
                                                 Entity target, HitResult hitResult) {
        if (!WlyModule.on("clickthrough") || world.isClientSide()
                || !(player instanceof ServerPlayer sp)
                || player.isSecondaryUseActive()
                || !(target instanceof ItemFrame)) {
            return InteractionResult.PASS;
        }
        Vec3 eye = sp.getEyePosition();
        Vec3 dir = sp.getViewVector(1.0F);
        BlockHitResult next = pickSkipping(world, sp, eye, dir, 8);
        if (next == null || next.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        var behindState = world.getBlockState(next.getBlockPos());
        var result = behindState.useItemOn(sp.getItemInHand(hand), world, sp, hand, next);
        if (result.consumesAction()) {
            return result;
        }
        return behindState.useWithoutItem(world, sp, next);
    }

    private static BlockHitResult pickSkipping(Level world, ServerPlayer sp, Vec3 eye, Vec3 dir, int depth) {
        if (depth <= 0) {
            return null;
        }
        var ctx = new ClipContext(eye, eye.add(dir.scale(8)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp);
        return world.clip(ctx);
    }
}
