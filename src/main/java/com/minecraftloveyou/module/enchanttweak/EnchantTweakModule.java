package com.minecraftloveyou.module.enchanttweak;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 轻松附魔（enchant-tweak）：
 * ① 冲突附魔可共存（exclusive_set 失效，同附魔仍拒绝）——本轮交付
 * ② 无限不耗任意箭 → 已由「所有箭配无限」模块覆盖
 * ③④ 弓弩附魔互通/修补扣经验 → 依赖 26.2 数据驱动附魔标签注入，下轮 spike
 */
public class EnchantTweakModule implements WlyModule {

    public static final String ID = "enchant-tweak";

    public static void register() {
        ModuleManager.register(new EnchantTweakModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "冲突附魔共存";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin
    }
}