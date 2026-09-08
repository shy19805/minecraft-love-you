package com.minecraftloveyou.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 键位基础设施（26.2）：
 * - 必须 KeyMappingHelper.registerKeyMapping 注册进 Options.keyMappings[]，
 *   否则不出现在"按键设置"、无法改键、不持久化。
 * - 键名用翻译键 key.minecraft_love_you.<id>（lang 提供中文显示）。
 * - 分类：key.category.minecraft_love_you.world_loves_you。
 */
public class WlyKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "world_loves_you"));

    private static final Map<String, KeyMapping> KEYS = new LinkedHashMap<>();

    private WlyKeys() {
    }

    /** 注册按键（幂等）。 */
    public static void register(String moduleId, String displayName, int defaultGlfwKey) {
        if (KEYS.containsKey(moduleId)) {
            return;
        }
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + WorldLovesYouMod.MOD_ID + "." + moduleId,
                InputConstants.Type.KEYSYM,
                defaultGlfwKey,
                CATEGORY));
        KEYS.put(moduleId, key);
        WorldLovesYouMod.LOGGER.info("[我的世界爱你] 注册按键：{} -> {}（默认键码 {}）", displayName, moduleId, defaultGlfwKey);
    }

    public static KeyMapping get(String moduleId) {
        return KEYS.get(moduleId);
    }

    public static Map<String, KeyMapping> all() {
        return KEYS;
    }

    /** 模块开启且按键按下。 */
    public static boolean active(String moduleId) {
        KeyMapping k = KEYS.get(moduleId);
        return k != null && WlyModule.on(moduleId) && k.isDown();
    }

    /**
     * 物理键状态（GLFW 直读）：容器 Screen 打开时 vanilla 不分发 setDown/click，
     * 该方法绕开，界面内按键也有效；自动跟随改键。
     */
    public static boolean isPhysicallyDown(String moduleId) {
        KeyMapping k = KEYS.get(moduleId);
        if (k == null || !WlyModule.on(moduleId)) {
            return false;
        }
        var bound = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(k);
        if (bound == null || bound == InputConstants.UNKNOWN
                || bound.getType() != InputConstants.Type.KEYSYM) {
            return false;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow(), bound.getValue());
    }
}