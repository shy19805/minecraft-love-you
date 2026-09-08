package com.minecraftloveyou.module.bigstacks;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 大堆叠（big-stacks）：物品堆叠上限 64 → 999（组件默认值统一被改写）。 */
public class BigStacksModule implements WlyModule {

    public static final String ID = "big-stacks";
    public static int maxStack = 999;

    public static void register() {
        ModuleManager.register(new BigStacksModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "大堆叠（999）";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin/ItemStackMaxStackMixin
    }
}
