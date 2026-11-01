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
    /**
     * 启用状态缓存：{@link #enabled(String)} 被 mixin 热路径调用
     * （物品堆叠上限查询、每次受击、每次投掷、每 tick 攀爬），原先每次都走
     * {@code synchronized} 的配置装载 + 两次哈希查找。改为首次解析后缓存，
     * 配置变更时用 {@link #invalidateEnabledCache()} 整体失效。
     */
    private static final Map<String, Boolean> ENABLED_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

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

    /** 模块是否启用：配置值 且 未被强制禁用（带缓存，热路径无锁）。 */
    public static boolean enabled(String id) {
        Boolean cached = ENABLED_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        boolean value = !FORCED_OFF.contains(id) && WlyConfigData.moduleEnabled(id);
        ENABLED_CACHE.put(id, value);
        return value;
    }

    /** 配置或强制禁用发生变化后调用：丢弃全部缓存，下次查询重新解析。 */
    public static void invalidateEnabledCache() {
        ENABLED_CACHE.clear();
    }

    /** 强制禁用（初始化失败时调用），并在内存中立即生效。 */
    public static void forceOff(String id) {
        FORCED_OFF.add(id);
        invalidateEnabledCache();
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