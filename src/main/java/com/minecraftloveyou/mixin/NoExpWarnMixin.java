package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 去实验性设置警告：让"数据包含实验功能"判定为 false → 直接进入世界创建。 */
@Mixin(CreateWorldScreen.class)
public abstract class NoExpWarnMixin {

    @Redirect(
            method = "tryApplyNewDataPacks",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/flag/FeatureFlags;isExperimental(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
    private static boolean wly$skipExperimentalWarn(FeatureFlagSet flags) {
        return WlyModule.on("no-exp-warn") ? false : FeatureFlags.isExperimental(flags);
    }
}