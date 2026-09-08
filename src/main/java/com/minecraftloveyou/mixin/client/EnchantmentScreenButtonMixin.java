package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.reroll.RerollModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 刷新附魔按钮：附魔台界面底部"⟳ 刷新附魔(H)"按钮区（60x16），
 * 鼠标点击或 H 键（可改）都触发。挂父类以 @Shadow 取 protected 坐标。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class EnchantmentScreenButtonMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void wly$renderRerollButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!WlyModule.on("reroll")) {
            return;
        }
        if (!((Object) this instanceof EnchantmentScreen self)) {
            return;
        }
        int x = leftPos + 72;
        int y = topPos + 160;
        boolean hover = mouseX >= x && mouseX < x + 60 && mouseY >= y && mouseY < y + 16;
        var text = hover ? 0xFFFFE08A : 0xFFE0C060;
        var label = Component.translatable("wly.ui.reroll.button");
        graphics.text(self.getFont(), label, x + 2, y + 4, text, true);
        if (hover) {
            graphics.setComponentTooltipForNextFrame(self.getFont(),
                    List.of(Component.translatable("wly.ui.reroll.tooltip")), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void wly$clickReroll(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick,
                                 org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!WlyModule.on("reroll")) {
            return;
        }
        if (!((Object) this instanceof EnchantmentScreen)) {
            return;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) {
            return;
        }
        int x = leftPos + 72;
        int y = topPos + 160;
        if (mouseX >= x && mouseX < x + 60 && mouseY >= y && mouseY < y + 16) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.getConnection() != null) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(RerollModule.RerollRequest.INSTANCE);
                cir.setReturnValue(true);
            }
        }
    }
}