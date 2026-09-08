package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.eatfast.EatFastModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 快吃：重定向 LivingEntity 内两处 ItemStack#getUseDuration 调用，
 * 仅对食物生效，把进食 tick 数除以速度倍率（0 → 1 tick 瞬吃）。
 */
@Mixin(LivingEntity.class)
public abstract class EatFastMixin {

    @Redirect(
            method = {"startUsingItem", "onSyncedDataUpdated"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int wly$fastEatDuration(ItemStack stack, LivingEntity user) {
        int original = stack.getUseDuration(user);
        if (WlyModule.on("eat-fast") && stack.has(DataComponents.FOOD)) {
            float speed = EatFastModule.eatSpeed;
            if (speed <= 0.01F) {
                return 1; // 0 = 瞬间吃完
            }
            return Math.max(1, (int) (original / speed));
        }
        return original;
    }
}