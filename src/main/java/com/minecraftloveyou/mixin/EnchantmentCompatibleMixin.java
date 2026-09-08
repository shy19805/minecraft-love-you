package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 轻松附魔①：冲突附魔可共存（exclusive_set 判定恒真；同附魔仍拒绝）。 */
@Mixin(Enchantment.class)
public abstract class EnchantmentCompatibleMixin {

    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void wly$alwaysCompatible(net.minecraft.core.Holder<Enchantment> a, net.minecraft.core.Holder<Enchantment> b, CallbackInfoReturnable<Boolean> cir) {
        if (WlyModule.on("enchant-tweak") && !a.equals(b)) {
            cir.setReturnValue(true);
        }
    }
}
