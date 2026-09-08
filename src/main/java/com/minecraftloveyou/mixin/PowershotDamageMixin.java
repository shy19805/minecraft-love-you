package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 力量射击·伤害子功能：投掷物（雪球/珍珠/鸡蛋等）命中生物时按初速追加伤害（0~50，默认关闭）。
 * 注：onHitEntity 声明在 Projectile 基类（子类覆写并调用 super，TAIL 注入生效）。
 */
@Mixin(Projectile.class)
public abstract class PowershotDamageMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void wly$projectileDamage(net.minecraft.world.phys.EntityHitResult hit, CallbackInfo ci) {
        if (!WlyModule.on("powershot") || !PowershotModule.damageEnabled) {
            return;
        }
        double v = PowershotModule.powerVelocity;
        if (v <= 0) {
            return;
        }
        Entity target = hit.getEntity();
        int dmg = (int) Math.min(PowershotModule.damageMax, Math.round(v * (PowershotModule.damageMax / 10.0)));
        if (dmg > 0) {
            Projectile self = (Projectile) (Object) this;
            target.hurt(target.damageSources().thrown(self, self.getOwner()), dmg);
        }
    }
}