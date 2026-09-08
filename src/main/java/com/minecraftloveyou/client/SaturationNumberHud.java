package com.minecraftloveyou.client;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.eatfull.EatFullModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 饱和度数字 HUD：显示在饥饿条右端外侧（模块 eat-full 开启时）。
 * 数值 = 玩家当前饱和度（FoodData.getSaturationLevel），1 位小数。
 */
public class SaturationNumberHud implements HudElement {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("minecraft_love_you", "saturation_number");
    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        // 兼容修改 HUD 的模组：独立根层（addLast），不参与 FOOD_BAR 重排链；
        // 位置按原版食物条标准坐标自算，HUD 模组覆盖/重排时本层保持稳定。
        HudElementRegistry.addLast(ID, new SaturationNumberHud());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        if (!WlyModule.on(EatFullModule.ID)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        float saturation = Mth.clamp(player.getFoodData().getSaturationLevel(), 0.0F, EatFullModule.saturationCap);
        String text = (saturation % 1.0F == 0.0F)
                ? String.valueOf((int) saturation)
                : String.format("%.1f", saturation);
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        // 饥饿条右端在 width/2 + 91，向下 10px 与图标行对齐（原版 food bar 基线 y = height - rightHeight）
        int x = w / 2 + 91 + 3;
        int y = h - 39;
        int color = 0xFFFFD24A; // 暖金色，和饱和度语义一致
        ctx.text(mc.font, text, x, y, color, true);
    }
}