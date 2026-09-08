package com.minecraftloveyou.module.powershot;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 力量射击（powershot）：
 * 投掷物初速倍率（0~10，0=没力气只扔脚底，10=十倍速）。
 * 可选子功能：投掷物命中造成伤害，伤害随初速增加（0~50）。
 */
public class PowershotModule implements WlyModule {

    public static final String ID = "powershot";
    /** 投掷物初速倍率（0~10，默认 2.5）。 */
    public static double powerVelocity = 2.5;
    /** 是否对命中生物造成伤害（默认关）。 */
    public static boolean damageEnabled = false;
    /** 伤害上限（0~50）。 */
    public static int damageMax = 50;

    public static void register() {
        ModuleManager.register(new PowershotModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "力量射击";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin/PowershotMixin + mixin/PowershotDamageMixin
    }
}