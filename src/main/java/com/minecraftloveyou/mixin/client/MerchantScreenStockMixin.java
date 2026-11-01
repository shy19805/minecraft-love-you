package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 显示村民全部交易（visible-traders）：缺货交易不再灰显（仅渲染层，不影响实际购买）。
 *
 * <p><b>注入点必须逐个点名，不能用 {@code method = "*"}</b>：
 * 26.1.2 / 26.2 的 {@code MerchantScreen} 里调用 {@code MerchantOffer#isOutOfStock()} 的只有三处
 * （已用 javap 在两版上核对，签名一致）：
 * <ul>
 *   <li>{@code extractBackground(GuiGraphicsExtractor, int, int, float)} —— 交易列表底色</li>
 *   <li>{@code extractContents(GuiGraphicsExtractor, int, int, float)} —— 交易条目本身</li>
 *   <li>{@code extractButtonArrows(GuiGraphicsExtractor, MerchantOffer, int, int)} —— 箭头按钮</li>
 * </ul>
 * 原来写 {@code method = "*", require = 0}：选择器一旦不被接受或方法改名，注入**静默失败**，
 * 功能看起来「装了但没反应」。现在点名 + {@code require = 1}，改错就当场报错。
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenStockMixin {

    @Redirect(method = "extractBackground", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;isOutOfStock()Z"))
    private boolean wly$neverOutOfStockBackground(MerchantOffer offer) {
        return !WlyModule.on("visible-traders") && offer.isOutOfStock();
    }

    @Redirect(method = "extractContents", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;isOutOfStock()Z"))
    private boolean wly$neverOutOfStockContents(MerchantOffer offer) {
        return !WlyModule.on("visible-traders") && offer.isOutOfStock();
    }

    @Redirect(method = "extractButtonArrows", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;isOutOfStock()Z"))
    private boolean wly$neverOutOfStockArrows(MerchantOffer offer) {
        return !WlyModule.on("visible-traders") && offer.isOutOfStock();
    }
}
