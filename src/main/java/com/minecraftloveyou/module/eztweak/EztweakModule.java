package com.minecraftloveyou.module.eztweak;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * Easy Tweak 规则集（第一批 4 条，各自独立开关，默认全开）：
 * 1) 禁末影人搬方块
 * 2) 禁苦力怕爆炸（完全无爆炸）
 * 3) 恶魂火球不破坏方块（保留伤害）
 * 4) 防耕地踩踏
 */
public class EztweakModule implements WlyModule {

    public static final String ID = "eztweak";
    public static boolean ruleEndermanGrief = true;
    public static boolean ruleCreeperNoExplode = true;
    public static boolean ruleGhastNoGrief = true;
    public static boolean ruleFarmlandProtect = true;

    public static void register() {
        ModuleManager.register(new EztweakModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Easy Tweak 规则集";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 4 个 mixin 中（门控在 mixin 内）
    }
}