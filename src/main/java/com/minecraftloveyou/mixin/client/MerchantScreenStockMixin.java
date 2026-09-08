package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 显示村民全部交易（visible-traders）：缺货交易不再灰显（仅渲染层，不影响实际购买）。 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenStockMixin {

    @Redirect(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/trading/MerchantOffer;isOutOfStock()Z"))
    private boolean wly$neverOutOfStock(MerchantOffer offer) {
        if (WlyModule.on("visible-traders")) {
            return false;
        }
        return offer.isOutOfStock();
    }
}
