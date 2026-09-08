package com.minecraftloveyou.module.openwater;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 始终开放水域：一格水也能钓到宝藏鱼获（解除 1.16+ 的钓鱼宝藏开放水域限制）。
 * 判定点改为恒真，见 mixin/OpenWaterMixin。
 */
public class OpenWaterModule implements WlyModule {

    public static final String ID = "open-water";

    public static void register() {
        ModuleManager.register(new OpenWaterModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "始终开放水域";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为全部在 mixin 中，这里仅占位（门控判断在 mixin 内）
    }
}