package com.minecraftloveyou.client;

import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** 世界爱你客户端入口：初始化各模块的客户端侧逻辑。 */
public class WorldLovesYouClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModuleManager.initAll(true);
        // 键位：作物连锁（默认 ` 键，可在按键设置修改）
        WlyKeys.register("harvest", "作物连锁（按住触发连锁）", 96);
        WlyKeys.register("sort", "一键整理", 82);   // R
        WlyKeys.register("reroll", "刷新附魔", 72); // H
        // 键状态 C2S 同步
        ChainKeySender.register();
        // 饱和度数字 HUD（独立根层，兼容 HUD 修改模组）
        SaturationNumberHud.register();
        // 一键整理按键 tick
        com.minecraftloveyou.module.sort.SortModule.registerClient();
        // 刷新附魔按键 tick
        com.minecraftloveyou.module.reroll.RerollModule.registerClient();
        // 实体渲染器（缺失会投掷即崩）
        EntityRenderers.register();
        // 墓碑 → Xaero 路点扫描器
        GraveWaypointScanner.register();
        // 手持耐久条
        // 战利品光束 / 弹道轨迹
        // 智能提示
        com.minecraftloveyou.module.smarttooltip.SmartTooltipModule.registerClient();
        // enchant-reveal S2C 接收
        EnchantRevealCache.register();
        // 客户端补充 /wly keys list
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                WlyCommands.register(dispatcher, true));
    }
}