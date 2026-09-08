package com.minecraftloveyou.module.eatfast;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 快吃：进食速度倍率（0~2，1=原版默认，2=两倍快，0=瞬间吃完）。
 * 实现点：mixin/EatFastMixin 重定向 ItemStack#getUseDuration（仅食物生效）。
 */
public class EatFastModule implements WlyModule {

    public static final String ID = "eat-fast";
    /** 进食速度倍率：1=原版，2=两倍快，0=瞬间。 */
    public static float eatSpeed = 1.0F;

    public static void register() {
        ModuleManager.register(new EatFastModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "快吃";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin
    }
}