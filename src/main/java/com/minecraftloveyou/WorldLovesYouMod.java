package com.minecraftloveyou;

import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 我的世界爱你模组主入口（服务端/双端）。
 * 所有功能经 ModuleManager 注册：单模块失败不影响其它模块。
 */
public class WorldLovesYouMod implements ModInitializer {
    public static final String MOD_ID = "minecraft_love_you";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        com.minecraftloveyou.module.demo.DemoModule.register();
        com.minecraftloveyou.module.maxhealthfix.MaxHealthFixModule.register();
        com.minecraftloveyou.module.ruler.RulerModule.register();
        com.minecraftloveyou.module.harvest.HarvestModule.register();
        com.minecraftloveyou.module.openwater.OpenWaterModule.register();
        com.minecraftloveyou.module.beaconfill.BeaconFillModule.register();
        com.minecraftloveyou.module.infarrows.InfArrowsModule.register();
        com.minecraftloveyou.module.eatfull.EatFullModule.register();
        com.minecraftloveyou.module.noexpwarn.NoExpWarnModule.register();
        com.minecraftloveyou.module.graves.GravesModule.register();
        com.minecraftloveyou.module.mendhold.MendHoldModule.register();
        com.minecraftloveyou.module.eztweak.EztweakModules.registerAll();
        com.minecraftloveyou.module.friendlyfire.FriendlyFireModule.register();
        com.minecraftloveyou.module.climbing.ClimbingModule.register();
        com.minecraftloveyou.module.splashmilk.SplashMilkModule.register();
        com.minecraftloveyou.module.eatfast.EatFastModule.register();
        com.minecraftloveyou.module.vineextend.VineExtendModule.register();
        com.minecraftloveyou.module.tamesafety.TameSafetyModule.register();
        com.minecraftloveyou.module.anvilmaster.AnvilMasterModule.register();
        com.minecraftloveyou.module.reroll.RerollModule.register();
        com.minecraftloveyou.module.enchantreveal.EnchantRevealModule.register();
        com.minecraftloveyou.module.enchanttweak.EnchantTweakModule.register();
        com.minecraftloveyou.module.visibletraders.VisibleTradersModule.register();
        com.minecraftloveyou.module.sort.SortModule.register();
        com.minecraftloveyou.module.elb.ElbModule.register();
        com.minecraftloveyou.module.emeraldlure.EmeraldLureModule.register();
        com.minecraftloveyou.module.rendi.RendiModule.register();
        com.minecraftloveyou.module.spawnerplus.SpawnerPlusModule.register();
        com.minecraftloveyou.module.nonconflict.NonConflictKeysModule.register();
        com.minecraftloveyou.module.bigstacks.BigStacksModule.register();
        com.minecraftloveyou.module.snowcap.SnowCapModule.register();
        com.minecraftloveyou.module.smarttooltip.SmartTooltipModule.register();
        com.minecraftloveyou.module.tradedisplay.TradeDisplayModule.register();
        com.minecraftloveyou.module.smoothbedrock.SmoothBedrockModule.register();
        com.minecraftloveyou.module.ctm.CtmModule.register();
        com.minecraftloveyou.module.powershot.PowershotModule.register();
        com.minecraftloveyou.module.clickthrough.ClickthroughModule.register();
        // 连锁键 C2S 同步通道
        com.minecraftloveyou.core.WlyNet.register();
        // 统一初始化（异常隔离）
        ModuleManager.initAll(false);
        // /wly 巡检
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                WlyCommands.register(dispatcher, false));
        // 服务端自检（仅 WLY_SELFTEST=1 时启用，无该环境变量零开销）
        if (System.getenv("WLY_SELFTEST") != null) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED
                    .register(server -> com.minecraftloveyou.SelfTest.run(server));
        }
        LOGGER.info("[我的世界爱你] 初始化完成，共注册 {} 个模块", ModuleManager.ids().size());
    }
}
