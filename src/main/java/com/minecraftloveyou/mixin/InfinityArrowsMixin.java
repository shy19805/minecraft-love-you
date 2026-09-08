package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无限附魔作用于所有箭种：只要武器带无限且弹药是箭，消耗量为 0。
 */
@Mixin(EnchantmentHelper.class)
public abstract class InfinityArrowsMixin {

    @Inject(method = "processAmmoUse", at = @At("HEAD"), cancellable = true)
    private static void wly$infinityAllArrows(
            ServerLevel serverLevel, ItemStack weapon, ItemStack ammo, int amount,
            CallbackInfoReturnable<Integer> cir) {
        if (!WlyModule.on("infinity-arrows") || weapon.isEmpty() || ammo.isEmpty() || !ammo.is(ItemTags.ARROWS)) {
            return;
        }
        try {
            ResourceKey<Enchantment> infinityKey = Enchantments.INFINITY;
            var holder = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(infinityKey);
            if (EnchantmentHelper.getItemEnchantmentLevel(holder, weapon) > 0) {
                cir.setReturnValue(0);
            }
        } catch (Exception ignored) {
            // 找不到附魔注册表时按原版行为（不拦截）
        }
    }
}