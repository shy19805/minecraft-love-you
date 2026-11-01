package com.minecraftloveyou.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 26.1.2 / 26.2 客户端兼容层。
 *
 * <p>26.2 把「当前屏幕」从 {@code Minecraft.screen} 公有字段搬到了 {@code Gui.screen()} 方法，
 * 源码里直接写任一种写法都会在另一版编译不过。这里用反射双路兜底，
 * 让同一份源码、同一个 jar 在两版都能取到当前 Screen。
 *
 * <p><b>仅客户端使用</b>：服务端代码不得引用本类（见《开发避坑手册》#1 专服类泄漏）。
 */
public final class ClientCompat {

    private static Method guiScreenMethod;
    private static Field mcScreenField;
    private static boolean resolved;

    private ClientCompat() {
    }

    /** 当前打开的 Screen；没有则返回 null。26.2 走 {@code Gui.screen()}，26.1.2 走 {@code Minecraft.screen} 字段。 */
    public static Screen currentScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        resolve();
        try {
            if (guiScreenMethod != null) {
                return (Screen) guiScreenMethod.invoke(mc.gui);
            }
            if (mcScreenField != null) {
                return (Screen) mcScreenField.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
            // 取值失败按「无屏幕」处理，不影响渲染
        }
        return null;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            guiScreenMethod = Gui.class.getMethod("screen");
        } catch (NoSuchMethodException e) {
            try {
                mcScreenField = Minecraft.class.getField("screen");
            } catch (NoSuchFieldException ignored) {
                // 两版都没有 → 兼容层失效，调用方按 null 处理
            }
        }
    }
}
