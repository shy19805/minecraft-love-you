package com.minecraftloveyou.client;

import com.minecraftloveyou.module.splashmilk.MilkSplashEntity;
import com.minecraftloveyou.module.splashmilk.SplashMilkModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** 实体渲染器注册（客户端）：不注册 = 投掷瞬间 renderer null → 崩溃。 */
public final class EntityRenderers {

    private static boolean registered = false;

    private EntityRenderers() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EntityRendererRegistry.register(SplashMilkModule.MILK_ENTITY, ThrownItemRenderer::new);
    }
}