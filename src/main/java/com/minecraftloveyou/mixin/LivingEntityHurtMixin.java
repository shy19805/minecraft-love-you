package com.minecraftloveyou.mixin;

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
 * invulnerableTime>10 的减伤分摊分支 → 连击全额伤害。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void wly$noInvulnFrames(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("rendi")) {
            ((LivingEntityInvulnAccessor) (Object) this).minecraftloveyou$setInvulnerableTime(0);
        }
    }
}
