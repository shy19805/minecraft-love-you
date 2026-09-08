package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 始终开放水域：开阔水域判定恒真（模块开启时）。
 */
@Mixin(FishingHook.class)
public abstract class OpenWaterMixin {

    @Inject(method = "calculateOpenWater", at = @At("HEAD"), cancellable = true)
    private void wly$alwaysOpenWater(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("open-water")) {
            cir.setReturnValue(true);
        }
    }
}