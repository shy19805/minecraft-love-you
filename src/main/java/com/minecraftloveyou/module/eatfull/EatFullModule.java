package com.minecraftloveyou.module.eatfull;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 满饱仍可吃：饥饿吃满也能继续吃（可配），饱和度增益允许溢出（上限可调）。
 * 入口点：mixin/EatWhenFullMixin（canEat 恒真）+ mixin/FoodSaturationMixin（饱和度夹取上限）。
 */
public class EatFullModule implements WlyModule {

    public static final String ID = "eat-full";
    /** 饱和度上限（默认 100；模组菜单文本框可改，最大 21 亿）。 */
    public static int saturationCap = 100;

    public static void register() {
        ModuleManager.register(new EatFullModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "满饱仍可吃";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin 中（门控在 mixin 内）
    }
}