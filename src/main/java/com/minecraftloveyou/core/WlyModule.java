package com.minecraftloveyou.core;

/**
 * 模块统一接口：世界爱你的每一个功能 = 一个 WlyModule 实现。
 * 约定：
 *  1) id() 唯一，对应配置 enabled 键名；
 *  2) 所有事件监听 / mixin 入口第一行必须做 enabled 门控（WlyModule.on(id)）；
 *  3) 模块之间不共享可变状态，单模块异常不影响其它模块。
 */
public interface WlyModule {

    /** 模块唯一 id（配置开关键名）。 */
    String id();

    /** 模块中文显示名（配置页与 /wly list 用）。 */
    String displayName();

    /** 是否仅客户端功能。 */
    default boolean clientOnly() {
        return false;
    }

    /** 默认开启（全部功能默认启动）。 */
    default boolean enabledByDefault() {
        return true;
    }

    /** 服务端/双端初始化。 */
    void init(ModuleContext ctx);

    /** 客户端初始化（默认空实现）。 */
    default void initClient(ModuleContext ctx) {
    }

    /** 便捷门控：模块是否处于启用状态（false 时所有 hook 必须直接返回）。 */
    static boolean on(String moduleId) {
        return ModuleManager.enabled(moduleId);
    }

    /** 便捷门控：当前模块是否启用。 */
    default boolean on() {
        return WlyModule.on(id());
    }
}