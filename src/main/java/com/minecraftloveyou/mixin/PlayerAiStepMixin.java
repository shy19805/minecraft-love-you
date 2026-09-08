package com.minecraftloveyou.mixin;

import com.minecraftloveyou.module.climbing.ClimbingModule;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 攀爬加成挂点：Player.aiStep TAIL。 */
@Mixin(Player.class)
public abstract class PlayerAiStepMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void wly$climbBoost(CallbackInfo ci) {
        ClimbingModule.applyClimbBoost((Player) (Object) this);
    }
}