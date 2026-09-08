package com.minecraftloveyou.module.visibletraders;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 显示村民全部交易：缺货交易不再灰显（仅客户端渲染层，不改变实际可购性）。 */
public class VisibleTradersModule implements WlyModule {

    public static final String ID = "visible-traders";

    public static void register() {
        ModuleManager.register(new VisibleTradersModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "显示村民全部交易";
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