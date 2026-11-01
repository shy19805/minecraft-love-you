package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.PlayerSide;
import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 力量射击：投掷物初速倍率（0~10）。
 *
 * <p><b>作用域：只对「玩家本人」与「玩家的宠物」投出的投掷物生效</b>——
 * 玩家手掷（雪球/鸡蛋/末影珍珠/药水/经验瓶/火焰弹/末影之眼）与
 * 宠物投射（雪傀儡的雪球、驯服羊驼的口水等）都走 {@code shootFromRotation}；
 * 怪物/发射器的投射物 owner 不是玩家，自动排除。
 *
 * <p><b>注入点说明</b>：挂 {@code Projectile#shootFromRotation}（实例方法），
 * 用投掷物自身的 {@code getOwner()} 判断来源，避免参数捕获的不确定性；
 * velocity 是局部变量 slot 5 —— 已用 javap 字节码核对：该方法把 {@code fload 5}
 * 作为 {@code shoot(...)} 的 velocity、{@code fload 6} 作为 inaccuracy（26.1.2 / 26.2 一致）。
 *
 * <p>箭类（{@code projectile.arrow.AbstractArrow}，含三叉戟）与烟花火箭不属于「投掷物」，保持原版。
 */
@Mixin(Projectile.class)
public abstract class PowershotMixin {

    @ModifyVariable(method = "shootFromRotation", at = @At("HEAD"), argsOnly = true, index = 5, require = 1)
    private float wly$boostThrowableVelocity(float velocity) {
        if (!WlyModule.on("powershot")) {
            return velocity;
        }
        Projectile self = (Projectile) (Object) this;
        if (self instanceof AbstractArrow || self instanceof FireworkRocketEntity) {
            return velocity; // 箭/三叉戟/烟花：不属于投掷物，保持原版
        }
        if (!PlayerSide.isPlayerOrPet(self.getOwner())) {
            return velocity; // 只服务玩家与玩家的宠物
        }
        return (float) (velocity * PowershotModule.powerVelocity);
    }
}