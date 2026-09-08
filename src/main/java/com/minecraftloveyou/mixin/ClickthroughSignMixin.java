package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;

/**
 * 点击穿透：告示牌（立牌/墙牌/悬挂/悬挂墙牌）右键直接放行 → 穿透到后方。
 * method 用同签名在四个类各自实现。
 */
@Mixin({net.minecraft.world.level.block.SignBlock.class,
        net.minecraft.world.level.block.WallSignBlock.class,
        net.minecraft.world.level.block.CeilingHangingSignBlock.class,
        net.minecraft.world.level.block.WallHangingSignBlock.class})
public abstract class ClickthroughSignMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void wly$passThroughNoItem(BlockState state, Level level, BlockPos pos, Player player,
                                       BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (WlyModule.on("clickthrough")) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void wly$passThroughItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (WlyModule.on("clickthrough") && !stack.isEmpty()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
