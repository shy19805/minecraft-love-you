package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 满饱仍可吃：canEat 恒真（模块开启时）。 */
@Mixin(Player.class)
public abstract class EatWhenFullMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void wly$canEatWhenFull(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("eat-full")) {
            cir.setReturnValue(true);
        }
    }
}