package com.minecraftloveyou.core;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.config.WlyConfigData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块注册中心与初始化调度器。
 * 注册：各功能包在 ModInitializer 阶段调用 register()；
 * 初始化：initAll(false)/initAll(true) 统一调用，单个模块抛异常只记日志并强制禁用，不影响其它模块。
 */
public class ModuleManager {

    private static final Map<String, WlyModule> MODULES = new LinkedHashMap<>();
    private static final java.util.Set<String> FORCED_OFF = new java.util.HashSet<>();

    private ModuleManager() {
    }

    /** 注册模块（重复 id 忽略并告警）。 */
    public static void register(WlyModule module) {
        if (MODULES.containsKey(module.id())) {
            WorldLovesYouMod.LOGGER.warn("[世界爱你] 模块 {} 重复注册，已忽略", module.id());
            return;
        }
        MODULES.put(module.id(), module);
        WorldLovesYouMod.LOGGER.info("[世界爱你] 注册模块：{}（{}）", module.displayName(), module.id());
    }

    /** 模块是否启用：配置值 且 未被强制禁用。 */
    public static boolean enabled(String id) {
        return !FORCED_OFF.contains(id) && WlyConfigData.moduleEnabled(id);
    }

    /** 强制禁用（初始化失败时调用），并在内存中立即生效。 */
    public static void forceOff(String id) {
        FORCED_OFF.add(id);
    }

    public static List<String> ids() {
        return new ArrayList<>(MODULES.keySet());
    }

    public static Map<String, WlyModule> modules() {
        return MODULES;
    }

    /** 初始化全部模块；client=false 跳过 clientOnly 模块，true 只跑 initClient。 */
    public static void initAll(boolean client) {
        ModuleContext ctx = new ModuleContext(client);
        for (WlyModule m : MODULES.values()) {
            if (FORCED_OFF.contains(m.id())) {
                continue;
            }
            try {
                if (client) {
                    m.initClient(ctx);
                } else {
                    if (!m.clientOnly()) {
                        m.init(ctx);
                    }
                }
            } catch (Throwable t) {
                WorldLovesYouMod.LOGGER.error("[世界爱你] 模块 {} 初始化失败，已自动禁用：{}", m.id(), t.toString());
                forceOff(m.id());
            }
        }
    }
}