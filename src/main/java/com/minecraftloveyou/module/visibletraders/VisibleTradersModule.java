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
        return "wly.module." + ID;
    }
    

    @Override
    public void init(ModuleContext ctx) {
        // 行为分两半：服务端 VillagerTradesMixin（把高级档交易补进 offers 并标为缺货，
        // 否则客户端根本收不到这些交易）+ 客户端 MerchantScreenStockMixin（缺货不灰显）。
        // 因此本模块不是 clientOnly。
    }
}