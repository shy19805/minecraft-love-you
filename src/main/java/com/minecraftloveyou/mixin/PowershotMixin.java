package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 力量射击：spawnProjectileFromRotation 的 pow 参数按可配置速度放大（0~10）。 */
@Mixin(Projectile.class)
public abstract class PowershotMixin {

    @ModifyVariable(method = "spawnProjectileFromRotation", at = @At("HEAD"), argsOnly = true, index = 5, require = 1)
    private static float wly$boostPow(float pow) {
        return WlyModule.on("powershot") ? (float) (pow * PowershotModule.powerVelocity) : pow;
    }
}