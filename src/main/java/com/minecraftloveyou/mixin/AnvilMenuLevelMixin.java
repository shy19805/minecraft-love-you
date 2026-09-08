package com.minecraftloveyou.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 附魔等级上限突破（elb）：铁砧合并时
 * `if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();`
 * Redirect getMaxLevel → Integer.MAX_VALUE（等级不再被钳制，合并=相加）。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuLevelMixin {

    /** 附魔书查 STORED_ENCHANTMENTS，物品查 ENCHANTMENTS。 */
    private static int levelOf(Holder<Enchantment> holder, ItemStack stack) {
        var stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) {
            int lv = stored.getLevel(holder);
            if (lv > 0) {
                return lv;
            }
        }
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    @Redirect(method = "createResult", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
    private int wly$noLevelCap(Enchantment enchantment) {
        if (WlyModule.on("elb")) {
            return Integer.MAX_VALUE;
        }
        return enchantment.getMaxLevel();
    }

    /**
     * elb：同附魔合并等级=相加（锋利 V+V=X）。
     * Wrap Mutable.set(holder, level)：level 由 vanilla 算好（same→+1 / else→max），
     * 我们在 elb 开启时改为 current + entry。current 从"被合并的输入两件"里取较大者。
     * 注意：此处拿不到原局部变量，改从 menu 的 inputSlots 读两件输入的附魔等级求和。
     */
    @WrapOperation(method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments$Mutable;set(Lnet/minecraft/core/Holder;I)V"))
    private void wly$sumLevels(ItemEnchantments.Mutable mutable, Holder<Enchantment> holder, int vanillaLevel,
                               Operation<Void> original) {
        if (!WlyModule.on("elb")) {
            original.call(mutable, holder, vanillaLevel);
            return;
        }
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack a = menu.getSlot(0).getItem(); // 输入
        ItemStack b = menu.getSlot(1).getItem(); // 材料
        int lvA = levelOf(holder, a);
        int lvB = levelOf(holder, b);
        if (lvA <= 0 && lvB <= 0) {
            original.call(mutable, holder, vanillaLevel); // 非合并路径（改名/修复）保底
            return;
        }
        int sum = Math.max(vanillaLevel, lvA + lvB);
        original.call(mutable, holder, sum);
    }
}