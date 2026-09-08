package com.minecraftloveyou.module.infarrows;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 所有箭配无限：带“无限”附魔的弓/弩使用任意箭种（药箭/光灵箭等）也不再消耗弹药。
 * 实际逻辑在 mixin/InfinityArrowsMixin（26.2 消耗统一走 EnchantmentHelper.processAmmoUse）。
 */
public class InfArrowsModule implements WlyModule {

    public static final String ID = "infinity-arrows";

    public static void register() {
        ModuleManager.register(new InfArrowsModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "所有箭配无限";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin 中
    }
}