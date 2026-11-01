package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** 交易显示：hover 交易按钮 → 追加完整摘要（挂 private 内部类，用字符串 target）。 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.MerchantScreen$TradeOfferButton")
public abstract class MerchantScreenTooltipMixin {

    @Shadow(remap = false)
    public int index;

    @Inject(method = "extractToolTip", at = @At("TAIL"))
    private void wly$tradeDetails(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (!WlyModule.on("trade-display")) {
            return;
        }
        // 26.2: mc.gui.screen()；26.1.2: mc.screen 字段 —— 走兼容层，同一 jar 两版通用
        if (!(com.minecraftloveyou.client.ClientCompat.currentScreen() instanceof MerchantScreen self)) {
            return;
        }
        var offers = self.getMenu().getOffers();
        int scrollOff = ((com.minecraftloveyou.mixin.client.MerchantScreenScrollAccessor) self).minecraftloveyou$getScrollOff();
        int offerIndex = this.index + scrollOff;
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return;
        }
        var offer = offers.get(offerIndex);
        var texts = new ArrayList<Component>();
        var sell = offer.getResult();
        texts.add(Component.translatable("wly.ui.trade.gains", sell.getCount(), sell.getHoverName())
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        var ench = sell.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
        if (ench != null) {
            ench.entrySet().forEach(e ->
                    texts.add(Component.translatable("wly.ui.trade.enchant",
                            net.minecraft.world.item.enchantment.Enchantment.getFullname(e.getKey(), e.getIntValue()))
                            .withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        var cost = net.minecraft.network.chat.Component.literal(offer.getCostA().getCount() + "× ")
                .append(offer.getCostA().getHoverName());
        if (!offer.getCostB().isEmpty()) {
            cost = cost.append(net.minecraft.network.chat.Component.literal(" + " + offer.getCostB().getCount() + "× "))
                    .append(offer.getCostB().getHoverName());
        }
        texts.add(Component.translatable("wly.ui.trade.costs", cost)
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        texts.add(offer.isOutOfStock()
                ? Component.translatable("wly.ui.trade.out_of_stock").withStyle(net.minecraft.ChatFormatting.RED)
                : Component.translatable("wly.ui.trade.available").withStyle(net.minecraft.ChatFormatting.GREEN));
        graphics.setComponentTooltipForNextFrame(self.getFont(), texts, xm, ym);
    }
}
