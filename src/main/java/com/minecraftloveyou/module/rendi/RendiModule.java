package com.minecraftloveyou.module.rendi;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 伤害免疫机制移除（rendi）：去掉受击无敌帧（26.2: hurtServer 中
 * invulnerableTime > 10 的分摊判定），连击全部全额伤害。
 * 实现：mixin/LivingEntityInvulnMixin Redirect clamp 前的判定。
 */
public class RendiModule implements WlyModule {

    public static final String ID = "rendi";

    public static void register() {
        ModuleManager.register(new RendiModule());
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
        // 行为在 mixin
    }
}