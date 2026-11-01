package com.minecraftloveyou.module.trajectory;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 弹道预测（trajectory）：参考 Projectiles Trajectory Prediction (PTP) ——
 * 手持可发射物品时，按原版弹道物理预测并画出飞行路径与落点。
 *
 * <p>本类**只放元数据**，不放任何逻辑：真正的实现在
 * {@code com.minecraftloveyou.client.TrajectoryRenderer}。
 * 原因是专服（dedicated server）环境不允许加载 {@code net.minecraft.client.*}，
 * 而 {@code ModuleManager.register()} 是在通用入口里跑的 —— 一旦本类引用了
 * {@code Minecraft} / {@code ClientLevel} 之类的类，专服启动就会直接崩在
 * 「Cannot load class net.minecraft.client... in environment type SERVER」。
 * （见《开发避坑手册》#1 专服类泄漏）
 */
public class TrajectoryModule implements WlyModule {

    public static final String ID = "trajectory";

    public static void register() {
        ModuleManager.register(new TrajectoryModule());
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
        // 客户端功能：实现在 client/TrajectoryRenderer（由 WorldLovesYouClient 注册）
    }
}
