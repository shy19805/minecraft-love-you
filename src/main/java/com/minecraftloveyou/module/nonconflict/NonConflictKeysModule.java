package com.minecraftloveyou.module.nonconflict;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 全键无冲（nonconflict-keys）：
 * - 按键分发层：26.2 的 KeyMapping.click/set 本就遍历所有同键映射（多激活无需改动）
 * - 视觉层：按键设置中同键绑定的"黄色冲突警告"消掉（mixin/client/KeyBindsListMixin）
 */
public class NonConflictKeysModule implements WlyModule {

    public static final String ID = "nonconflict-keys";

    public static void register() {
        ModuleManager.register(new NonConflictKeysModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "全键无冲";
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 client mixin
    }
}