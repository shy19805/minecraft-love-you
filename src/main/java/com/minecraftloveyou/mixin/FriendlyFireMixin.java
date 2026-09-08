package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 友军保护（模块 friendly-fire）：
 * - 目标是宠物 && 攻击者是主人或同主宠物 → 取消
 * - 目标是幼年动物 && 攻击者非潜行玩家 → 取消
 */
@Mixin(LivingEntity.class)
public abstract class FriendlyFireMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void wly$friendlyFireGuard(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!WlyModule.on("friendly-fire")) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        var cfg = com.minecraftloveyou.module.friendlyfire.FriendlyFireModule.class;
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        LivingEntity attackerLiving = attacker instanceof LivingEntity le ? le : null;

        // 1) 宠物保护
        if (com.minecraftloveyou.module.friendlyfire.FriendlyFireModule.protectPets
                && self instanceof TamableAnimal pet
                && pet.isTame()) {
            LivingEntity owner = pet.getOwner();
            if (owner != null && attackerLiving != null) {
                boolean attackerIsOwner = attackerLiving == owner;
                boolean attackerIsPetOfSameOwner =
                        attackerLiving instanceof TamableAnimal at && at.isTame() && at.getOwner() == owner;
                if (attackerIsOwner || attackerIsPetOfSameOwner) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        // 2) 幼年动物保护
        if (com.minecraftloveyou.module.friendlyfire.FriendlyFireModule.protectBabies
                && self instanceof Animal animal
                && animal.isBaby()
                && attackerLiving instanceof net.minecraft.world.entity.player.Player player) {
            boolean bypass = com.minecraftloveyou.module.friendlyfire.FriendlyFireModule.sneakBypass
                    && player.isShiftKeyDown();
            if (!bypass) {
                cir.setReturnValue(false);
            }
        }
    }
}