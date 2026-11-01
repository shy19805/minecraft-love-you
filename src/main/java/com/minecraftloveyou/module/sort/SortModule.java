package com.minecraftloveyou.module.sort;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 一键整理（sort）：背包与所有箱子，默认 R 键（可改），按创造物品栏排列方式整理。
 *
 * <p>客户端按键逻辑在 {@code client/SortKeyHandler}（本项目铁律 8：
 * 本类由通用入口注册，不得引用 {@code net.minecraft.client.*}）；服务端整理在
 * {@code core/SortPayload#handle}。
 */
public class SortModule implements WlyModule {

    public static final String ID = "sort";

    public static void register() {
        ModuleManager.register(new SortModule());
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
        // 交互在客户端 tick（按键）+ 服务端 payload
    }
}
