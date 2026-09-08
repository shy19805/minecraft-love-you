package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 大堆叠数字显示（big-stacks 配套）：
 * 1000 → "1K"，1000000 → "1M"（科学计数法缩写）；
 * 缩写文本 3 位及以上自动缩小字号（0.7 倍），避免超出物品格。
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class ItemCountDisplayMixin {

    @Redirect(
            method = "itemCount",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"))
    private void wly$smartCount(GuiGraphicsExtractor self, Font font, String amount, int x, int y, int color, boolean shadow) {
        if (!WlyModule.on("big-stacks")) {
            self.text(font, amount, x, y, color, shadow);
            return;
        }
        int count = parseCount(amount);
        String display = count >= 1_000_000 ? compactM(count) : count >= 1000 ? compactK(count) : amount;
        int len = display.length();
        // 重算右对齐：原 x 是"格子右缘 - 原字符串宽度"，用新字符串宽度重新右对齐到同一右缘
        int rightEdge = x + font.width(amount);
        int newX = rightEdge - font.width(display);
        if (len >= 3) {
            var pose = self.pose();
            pose.pushMatrix();
            pose.translate(newX + 16, y + 9);
            float scale = len >= 4 ? 0.55F : 0.7F;
            pose.scale(scale, scale);
            pose.translate(-(newX + 16), -(y + 9));
            try {
                self.text(font, display, newX, y, color, shadow);
            } finally {
                pose.popMatrix();
            }
        } else {
            self.text(font, display, newX, y, color, shadow);
        }
    }

    private static int parseCount(String amount) {
        try {
            return Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String compactK(int n) {
        int k = n / 1000;
        int rem = (n % 1000) / 100;
        return rem == 0 ? k + "K" : (k + "." + rem) + "K";
    }

    private static String compactM(int n) {
        long m = n / 1_000_000L;
        int rem = (n % 1_000_000) / 100_000;
        return rem == 0 ? m + "M" : (m + "." + rem) + "M";
    }
}