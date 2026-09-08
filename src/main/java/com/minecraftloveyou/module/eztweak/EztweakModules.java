package com.minecraftloveyou.module.eztweak;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 规则模块工厂：把原 "eztweak 规则集" 拆为 4 个独立开关模块。 */
public final class EztweakModules {

    public static final String RULE_ENDERMAN = "eztweak-enderman-grief";
    public static final String RULE_CREEPER = "eztweak-creeper-no-explode";
    public static final String RULE_GHAST = "eztweak-ghast-no-grief";
    public static final String RULE_FARMLAND = "eztweak-farmland-protect";

    private EztweakModules() {
    }

    private static void reg(WlyModule m) {
        ModuleManager.register(m);
    }

    public static void registerAll() {
        reg(new Base(RULE_ENDERMAN, "末影人不可搬方块"));
        reg(new Base(RULE_CREEPER, "苦力怕不爆炸"));
        reg(new Base(RULE_GHAST, "恶魂火球不破坏方块"));
        reg(new Base(RULE_FARMLAND, "耕地防踩踏"));
    }

    private static class Base implements WlyModule {
        private final String id;
        private final String name;

        Base(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return name;
        }

        @Override
        public boolean enabledByDefault() {
            return false; // 规则类默认关闭，避免误伤原版行为
        }

        @Override
        public void init(ModuleContext ctx) {
            // 行为在各自 mixin
        }
    }

    // 兼容旧配置键（若有用户已关 eztweak → 映射到 4 个子规则：保持全开默认）
    public static boolean on(String ruleId) {
        return WlyModule.on(ruleId);
    }
}