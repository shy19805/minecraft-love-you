package com.minecraftloveyou.client;

import com.minecraftloveyou.core.SortPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * 一键整理的**客户端**按键处理。
 *
 * <p>为什么单独放一个类：`SortModule` 由通用入口（双端都跑）注册，
 * 按项目铁律 8「通用入口模块类不得引用 {@code net.minecraft.client.*}」——
 * 客户端类型一旦落在那个类的方法体里，专服加载时可能触发
 * {@code Cannot load class ... in environment type SERVER}。
 * 这里把客户端逻辑隔离到 client 包，`SortModule` 只留元数据。
 */
public final class SortKeyHandler {

    private static boolean lastDown = false;

    private SortKeyHandler() {
    }

    /** 客户端：物理键上升沿触发（容器屏内 vanilla 不分发 click，必须 GLFW 直读）。 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = WlyKeys.isPhysicallyDown(com.minecraftloveyou.module.sort.SortModule.ID);
            if (down && !lastDown) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.containerMenu != null && mc.getConnection() != null) {
                    ClientPlayNetworking.send(SortPayload.INSTANCE);
                    com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[我的世界爱你][sort] 发送整理请求");
                }
            }
            lastDown = down;
        });
    }
}
