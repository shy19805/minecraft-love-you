package com.minecraftloveyou.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.EnchantmentMenu;

/**
 * 刷新附魔（reroll）的**客户端**按键处理。
 *
 * <p>单独成类的原因同 {@link SortKeyHandler}：`RerollModule` 由通用入口注册，
 * 不能在自己的方法体里引用 {@code net.minecraft.client.*}。
 */
public final class RerollKeyHandler {

    private static boolean lastDown = false;

    private RerollKeyHandler() {
    }

    /** 客户端：附魔台界面内物理键上升沿触发（vanilla 不分发界面内 click）。 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = WlyKeys.isPhysicallyDown(com.minecraftloveyou.module.reroll.RerollModule.ID);
            if (down && !lastDown) {
                if (client.player != null && client.getConnection() != null
                        && client.player.containerMenu instanceof EnchantmentMenu) {
                    ClientPlayNetworking.send(com.minecraftloveyou.module.reroll.RerollModule.RerollRequest.INSTANCE);
                    com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[我的世界爱你][reroll] 按键触发重掷请求");
                }
            }
            lastDown = down;
        });
    }
}
