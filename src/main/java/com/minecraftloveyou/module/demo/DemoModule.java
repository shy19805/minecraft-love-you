package com.minecraftloveyou.module.demo;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 示例模块：展示 WlyModule 的最小实现。B1 起各功能模块照此模板编写。
 */
public class DemoModule implements WlyModule {

    /** 在 ModInitializer 阶段注册（WorldLovesYouMod 会调用）。 */
    public static void register() {
        ModuleManager.register(new DemoModule());
    }

    @Override
    public String id() {
        return "demo";
    }

    @Override
    public String displayName() {
        return "示例模块";
    }

    @Override
    public void init(ModuleContext ctx) {
        if (!WlyModule.on(id())) {
            return; // 配置关闭：不做任何事
        }
        WorldLovesYouMod.LOGGER.info("[世界爱你] 示例模块已初始化（框架自检）");
    }
}