package com.minecraftloveyou.mixin;

import com.minecraftloveyou.module.graves.GraveHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 墓碑：在 ServerPlayer#die 最开头摘走全部背包+装备+经验到暂存（GraveHolder），
 * 原版掉落（dropAll）见到的背包已是空 → 物品不再散落；随后 GravesModule 用暂存生成墓碑。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDieMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void wly$captureInventoryBeforeDrop(DamageSource source, CallbackInfo ci) {
        if (com.minecraftloveyou.core.WlyModule.on("graves")) {
            GraveHolder.capture((ServerPlayer) (Object) this);
        }
    }
}