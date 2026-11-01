package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.PlayerSide;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * rendi：hurtServer 尾部把 invulnerableTime 清零 → 下一次受击不再走
 * invulnerableTime&gt;10 的减伤分摊分支 → 连击全额伤害。
 *
 * <p><b>作用域：只对「非玩家阵营」的生物生效。</b>玩家本人与玩家的宠物
 * （{@link PlayerSide}：玩家 / 驯服动物 / 雪傀儡）**保留原版受击无敌帧**，
 * 避免自己与宠物被连击秒杀；其余生物（怪物、村民、铁傀儡等）照旧清零。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void wly$noInvulnFrames(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!WlyModule.on("rendi")) {
            return;
        }
        // 玩家与玩家的宠物保留无敌帧
        if (PlayerSide.isPlayerOrPet((LivingEntity) (Object) this)) {
            return;
        }
        ((LivingEntityInvulnAccessor) (Object) this).minecraftloveyou$setInvulnerableTime(0);
    }
}
