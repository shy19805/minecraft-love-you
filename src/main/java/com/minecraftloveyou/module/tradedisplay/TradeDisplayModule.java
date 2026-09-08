package com.minecraftloveyou.module.tradedisplay;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 交易显示（trade-display）：hover 村民交易按钮显示完整摘要（含附魔详情）。 */
public class TradeDisplayModule implements WlyModule {

    public static final String ID = "trade-display";

    public static void register() {
        ModuleManager.register(new TradeDisplayModule());
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
    }
}
