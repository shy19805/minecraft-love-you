package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 末影人不可搬方块：private 内部类用字符串 targets 指定。 */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 1)
    private void wly$noTakeBlock(CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("eztweak-enderman-grief")) {
            cir.setReturnValue(false);
        }
    }
}
