package com.minecraftloveyou.module.noexpwarn;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 禁用实验性设置警告：创建世界数据包界面确认时直接应用（mixin/NoExpWarnMixin）。 */
public class NoExpWarnModule implements WlyModule {

    public static final String ID = "no-exp-warn";

    public static void register() {
        ModuleManager.register(new NoExpWarnModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "wly.module." + ID;
    }
    

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在客户端 mixin
    }
}