package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 绂佹湯褰变汉鎼�鏂瑰潡锛歱rivate 鍐呴儴绫荤敤瀛楃�︿�?targets銆?*/
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
    private void wly$noTakeBlock(CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("eztweak-enderman-grief")) {
            cir.setReturnValue(false);
        }
    }
}
