package com.minecraftloveyou.module.elb;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 附魔等级上限突破（elb）：铁砧合并不按 maxLevel 钳制（合并等级=相加）。 */
public class ElbModule implements WlyModule {

    public static final String ID = "elb";

    public static void register() {
        ModuleManager.register(new ElbModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "附魔等级上限突破";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin/AnvilMenuLevelMixin
    }
}
