package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 苦力怕不爆炸：explodeCreeper 直接取消（无伤害、无破坏、无音效）。 */
@Mixin(Creeper.class)
public abstract class CreeperExplodeMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void wly$noCreeperExplode(CallbackInfo ci) {
        if (WlyModule.on("eztweak-creeper-no-explode")) {
            ci.cancel();
        }
    }
}
