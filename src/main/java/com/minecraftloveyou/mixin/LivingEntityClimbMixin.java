package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.climbing.ClimbingModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.2 攀爬：handleOnClimbable 对下行 y clamp 到 -0.15（上行不限速）。
 * Redirect 该 Math.max，把下行下限放大 climbSpeed 倍 → 下行加速与上行一致。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbMixin {

    @Redirect(method = "handleOnClimbable", require = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"))
    private double wly$climbDownUnclamp(double a, double b) {
        if (WlyModule.on("climbing")) {
            double speed = Math.max(0.0, Math.min(10.0, ClimbingModule.climbSpeed));
            return Math.max(a, -0.15 * speed);
        }
        return Math.max(a, b);
    }
}