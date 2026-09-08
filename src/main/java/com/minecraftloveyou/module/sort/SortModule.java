package com.minecraftloveyou.module.sort;

import com.minecraftloveyou.client.WlyKeys;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.SortPayload;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** 一键整理（sort）：背包与所有箱子，默认 R 键（可改），按创造物品栏排列方式整理。 */
public class SortModule implements WlyModule {

    public static final String ID = "sort";

    public static void register() {
        ModuleManager.register(new SortModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "一键整理";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 交互在客户端 tick（按键）+ 服务端 payload
    }

    private static boolean lastDown = false;

    /** 客户端：物理键上升沿触发（容器屏内 vanilla 不分发 click，必须 GLFW 直读）。 */
    public static void registerClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = WlyKeys.isPhysicallyDown(ID);
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