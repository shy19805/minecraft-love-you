package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.eatfull.EatFullModule;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 饱和度溢出：eat 结束后把饱和度按上限重设（不依赖 clamp ordinal）。
 */
@Mixin(FoodData.class)
public abstract class FoodSaturationMixin {

    /**
     * 26.2 的饱和度 clamp 在私有 add(IF)V 里：
     * this.saturationLevel = Mth.clamp(saturation + saturationLevel, 0.0F, this.foodLevel);
     * Redirect ordinal=1 的 clamp，把上限 foodLevel 换成配置的 cap（≤20 时不改变行为）。
     */
    @Redirect(method = "add", require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0))
    private float wly$saturationCap(float value, float min, float max) {
        if (WlyModule.on("eat-full")) {
            max = Math.max(max, (float) EatFullModule.saturationCap);
        }
        return Mth.clamp(value, min, max);
    }
}