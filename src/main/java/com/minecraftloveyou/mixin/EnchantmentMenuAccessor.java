package com.minecraftloveyou.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** EnchantmentMenu 字段访问（reroll 用）。纯 @Accessor（不得含 default 方法）。 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Accessor("enchantmentSeed")
    DataSlot minecraftloveyou$getSeedSlot();

    @Accessor("enchantSlots")
    Container minecraftloveyou$getEnchantSlots();
}
