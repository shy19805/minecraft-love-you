package com.minecraftloveyou.module.splashmilk;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * 喷溅牛奶：投掷型奶瓶，命中清除范围内全部状态效果。
 * 注册：物品（进创造栏"杂项"）+ 实体 + 合成（奶桶+火药）。
 */
public class SplashMilkModule implements WlyModule {

    public static final String ID = "splash-milk";
    public static double splashRange = 3.0;

    public static Item SPLASH_MILK_ITEM;
    public static EntityType<MilkSplashEntity> MILK_ENTITY;

    public static void register() {
        registerContent();
        ModuleManager.register(new SplashMilkModule());
    }

    private static synchronized void registerContent() {
        if (SPLASH_MILK_ITEM != null) {
            return;
        }
        Identifier itemId = Identifier.fromNamespaceAndPath("minecraft_love_you", "splash_milk");
        SPLASH_MILK_ITEM = Registry.register(BuiltInRegistries.ITEM, itemId,
                new SplashMilkItem(new Item.Properties().stacksTo(16)
                        .setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, itemId))));
        MILK_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, itemId,
                EntityType.Builder.<MilkSplashEntity>of(MilkSplashEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(4)
                        .updateInterval(10)
                        .build(ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, itemId)));
        // 创造栏：加进"杂项"与"战斗"两个页签方便寻找
        CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(SPLASH_MILK_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.COMBAT)
                .register(entries -> entries.accept(SPLASH_MILK_ITEM));
        com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[世界爱你] 喷溅牛奶注册完成（物品+实体+创造栏）");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "喷溅牛奶";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 配方由 data/minecraft_love_you/recipe/splash_milk.json 提供
    }
}