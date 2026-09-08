package com.minecraftloveyou.mixin;

import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** EnchantmentMenu.getEnchantmentList Invoker（enchant-reveal 用）。 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuInvoker {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> minecraftloveyou$getEnchantmentList(
            net.minecraft.core.RegistryAccess access, ItemStack stack, int slot, int cost);
}
