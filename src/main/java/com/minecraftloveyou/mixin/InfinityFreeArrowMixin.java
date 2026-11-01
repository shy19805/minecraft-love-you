package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.infarrows.InfArrowsModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无限附魔免箭矢：背包里一支箭都没有时，带「无限」的弓/弩照样能上弦发射。
 *
 * 注入点选 Player.getProjectile 而不是 BowItem：javap 26.1.2 确认
 * BowItem.use（能否开始拉弓）与 BowItem.releaseUsing（能否射出）两处
 * 都以「getProjectile 返回 EMPTY」作为唯一的"没弹药"判据，补在这里一处即可全盖住。
 * 返回的是一次性 ItemStack，不在玩家背包里，因此永远不会真的扣掉玩家的东西。
 */
@Mixin(Player.class)
public abstract class InfinityFreeArrowMixin {

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void wly$infinityWithoutArrowStock(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) {
            return; // 原版已经翻到真弹药（含创造模式白送的那支），优先用真弹药
        }
        if (!WlyModule.on("infinity-arrows") || !InfArrowsModule.freeArrowWithoutStock) {
            return;
        }
        if (!(weapon.getItem() instanceof ProjectileWeaponItem)) {
            return;
        }
        Player self = (Player) (Object) this;
        try {
            ResourceKey<Enchantment> infinityKey = Enchantments.INFINITY;
            var holder = self.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(infinityKey);
            if (EnchantmentHelper.getItemEnchantmentLevel(holder, weapon) > 0) {
                cir.setReturnValue(new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.ARROW));
            }
        } catch (Exception ignored) {
            // 取不到附魔注册表时退回原版行为（没箭就是不能射）
        }
    }
}
