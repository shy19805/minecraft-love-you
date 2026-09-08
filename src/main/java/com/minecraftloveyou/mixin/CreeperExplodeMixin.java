package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 绂佽嫤鍔涙�曠垎鐐革細explodeCreeper 鐩存帴鍙栨秷锛堟棤浼ゅ�炽�佹棤鐮村潖銆佹棤闊虫晥鐖嗙偢锛夈�?*/
@Mixin(Creeper.class)
public abstract class CreeperExplodeMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void wly$noCreeperExplode(CallbackInfo ci) {
        if (WlyModule.on("eztweak-creeper-no-explode")) {
            ci.cancel();
        }
    }
}
