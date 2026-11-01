package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.PlayerSide;
import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 力量射击·伤害子功能：投掷物命中生物时按初速追加伤害（0~50，默认关闭）。
 *
 * <p><b>作用域与速度侧一致</b>：只对玩家本人与玩家宠物的投掷物生效；
 * 箭类（{@code projectile.arrow.AbstractArrow}，含三叉戟）与烟花火箭不属于投掷物，不追加伤害。
 *
 * <p>注：onHitEntity 声明在 Projectile 基类（子类覆写并调用 super，TAIL 注入生效）。
 */
@Mixin(Projectile.class)
public abstract class PowershotDamageMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void wly$projectileDamage(EntityHitResult hit, CallbackInfo ci) {
        if (!WlyModule.on("powershot") || !PowershotModule.damageEnabled) {
            return;
        }
        Projectile self = (Projectile) (Object) this;
        if (self instanceof AbstractArrow || self instanceof FireworkRocketEntity) {
            return; // 箭/三叉戟/烟花：不属于投掷物
        }
        if (!PlayerSide.isPlayerOrPet(self.getOwner())) {
            return; // 只服务玩家与玩家的宠物
        }
        double v = PowershotModule.powerVelocity;
        if (v <= 0) {
            return;
        }
        Entity target = hit.getEntity();
        int dmg = (int) Math.min(PowershotModule.damageMax, Math.round(v * (PowershotModule.damageMax / 10.0)));
        if (dmg > 0) {
            target.hurt(target.damageSources().thrown(self, self.getOwner()), dmg);
        }
    }
}