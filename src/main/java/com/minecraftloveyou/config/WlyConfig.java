package com.minecraftloveyou.config;

import com.minecraftloveyou.core.ModuleManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 配置 GUI 门面（Cloth Config）：仅客户端加载，专服不会触碰。
 * 数据层在 WlyConfigData（零客户端依赖）。
 */
public final class WlyConfig {

    private WlyConfig() {
    }

    /** 模块分组：id → 分组名（4 组；名称相近的功能同组，联动项在开关下方相邻）。 */
        private static final java.util.LinkedHashMap<String, String> GROUP_RULES = new java.util.LinkedHashMap<>() {{
        // ── 战斗与规则 ──
        put("powershot", "战斗与规则");
        put("friendly-fire", "战斗与规则");
        put("infinity-arrows", "战斗与规则");
        put("climbing", "战斗与规则");
        put("eztweak-creeper-no-explode", "战斗与规则");
        put("eztweak-ghast-no-grief", "战斗与规则");
        put("eztweak-enderman-grief", "战斗与规则");
        put("eztweak-farmland-protect", "战斗与规则");
        put("rendi", "战斗与规则");
        put("elb", "战斗与规则");
        put("anvil-master", "战斗与规则");
        put("reroll", "战斗与规则");
        put("enchant-reveal", "战斗与规则");
        put("enchant-tweak", "战斗与规则");
        put("nonconflict-keys", "战斗与规则");
        // ── 农业与生存 ──
        put("harvest", "农业与生存");
        put("eat-full", "农业与生存");
        put("eat-fast", "农业与生存");
        put("graves", "农业与生存");
        put("maxhealth-fix", "农业与生存");
        put("mend-hold", "农业与生存");
        put("beacon-fill", "农业与生存");
        put("vine-extend", "农业与生存");
        put("open-water", "农业与生存");
        put("snow-cap", "农业与生存");
        put("smooth-bedrock", "农业与生存");
        // ── 物品与便利 ──
        put("big-stacks", "物品与便利");
        put("sort", "物品与便利");
        put("clickthrough", "物品与便利");
        put("splash-milk", "物品与便利");
        put("ruler", "物品与便利");
        put("no-exp-warn", "物品与便利");
        put("visible-traders", "物品与便利");
        put("smart-tooltip", "物品与便利");
        put("trade-display", "物品与便利");
        put("ctm", "物品与便利");
        // ── 生物与探索 ──
        put("tame-safety", "生物与探索");
        put("emerald-lure", "生物与探索");
        put("spawner-plus", "生物与探索");
    }};

    private static final String GROUP_OTHER = "其他";

    private static String groupOf(String id) {
        return GROUP_RULES.getOrDefault(id, GROUP_OTHER);
    }

    /** 构建 Cloth Config 界面（Mod Menu 入口调用）。 */
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("我的世界爱你"));

        // 按分组聚合模块（顺序 = GROUP_RULES 声明顺序，未知模块进"其他"）
        java.util.LinkedHashMap<String, java.util.List<com.minecraftloveyou.core.WlyModule>> grouped = new java.util.LinkedHashMap<>();
        java.util.List<String> order = new java.util.ArrayList<>();
        for (var m : ModuleManager.modules().values()) {
            String g = groupOf(m.id());
            if (!order.contains(g)) {
                order.add(g);
            }
            grouped.computeIfAbsent(g, k -> new java.util.ArrayList<>()).add(m);
        }
        for (var entry : grouped.entrySet()) {
            ConfigCategory cat = builder.getOrCreateCategory(Component.literal(entry.getKey()));
            for (var m : entry.getValue()) {
                String id = m.id();
                boolean cur = WlyConfigData.isStored(id)
                        ? WlyConfigData.moduleEnabled(id)
                        : m.enabledByDefault();
                cat.addEntry(builder.entryBuilder()
                        .startBooleanToggle(Component.literal(m.displayName() + "（" + id + "）"), cur)
                        .setDefaultValue(m.enabledByDefault())
                        .setTooltip(Component.literal("关闭后该功能完全失效，与未安装一致"))
                        .setSaveConsumer(v -> WlyConfigData.put(id, v))
                        .build());

                // 联动配置项：力量射击开关下方紧邻初速数值 + 伤害开关/上限
                if (id.equals("powershot")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.literal("力量射击·初速倍率"),
                                    (float) com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity)
                            .setMin(0.0F).setMax(10.0F)
                            .setDefaultValue(2.5F)
                            .setTooltip(Component.literal("投掷物初速倍率（0~10）：0=完全没力气只扔脚底，1=原版，10=十倍速度。"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.literal("投掷物伤害"),
                                    com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled)
                            .setDefaultValue(false)
                            .setTooltip(Component.literal("开启后投掷物命中生物造成伤害，伤害随初速增加（范围 0~50）。默认关闭。"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.literal("投掷物伤害上限"),
                                    com.minecraftloveyou.module.powershot.PowershotModule.damageMax)
                            .setMin(0).setMax(50)
                            .setDefaultValue(50)
                            .setTooltip(Component.literal("投掷物可造成的最大伤害（0~50），实际伤害=初速×上限/10。"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.damageMax = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

            // 联动配置项：大堆叠开关下方紧邻堆叠上限
                if (id.equals("big-stacks")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.literal("堆叠上限"), com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack)
                            .setMin(1)
                            .setMax(9_999_999)
                            .setDefaultValue(999)
                            .setTooltip(Component.literal("物品堆叠上限（默认 999，1 = 恢复原版）。"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack = Math.max(1, v);
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：满饱仍可吃开关下方紧邻饱和度上限
                if (id.equals("eat-full")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.literal("饱和度上限"), com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap)
                            .setMin(0)
                            .setMax(2_100_000_000)
                            .setDefaultValue(100)
                            .setTooltip(Component.literal("饱和度可积累到的最大值（0 ~ 2,100,000,000，默认 100）。"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：右键收获并补种开关下方紧邻三个连锁参数
                if (id.equals("harvest")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.literal("连锁上限（收获/催熟）"), com.minecraftloveyou.module.harvest.HarvestModule.chainLimit)
                            .setMin(1).setMax(4096)
                            .setDefaultValue(64)
                            .setTooltip(Component.literal("连锁收获与骨粉连锁催熟的最大作物数（默认 64）"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.chainLimit = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.literal("连锁仅同种作物"), com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies)
                            .setDefaultValue(true)
                            .setTooltip(Component.literal("开启=只连锁同种作物；关闭=连锁所有作物"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.literal("连锁种植上限"), com.minecraftloveyou.module.harvest.HarvestModule.plantLimit)
                            .setMin(1).setMax(4096)
                            .setDefaultValue(64)
                            .setTooltip(Component.literal("手持种子右键耕地时连锁种植的空耕地数上限（默认 64）"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.plantLimit = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：快吃开关下方紧邻进食速度
                if (id.equals("eat-fast")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.literal("进食速度倍率"), com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed)
                            .setMin(0.0F).setMax(2.0F)
                            .setDefaultValue(1.0F)
                            .setTooltip(Component.literal("1 = 原版速度；2 = 两倍快；0 = 瞬间吃完（范围 0~2）"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：更好的攀爬开关下方紧邻攀爬速度 + 下滑
                if (id.equals("climbing")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.literal("攀爬速度倍率"), (float) com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed)
                            .setMin(0.0F).setMax(10.0F)
                            .setDefaultValue(2.0F)
                            .setTooltip(Component.literal("1 = 原版速度；2 = 两倍；0 = 完全爬不动（范围 0~10，默认 2）"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.literal("俯视加速下滑"), com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost)
                            .setDefaultValue(true)
                            .setTooltip(Component.literal("攀爬中低头 60° 以上时加速下滑"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost = v;
                                WlyConfigData.save();
                            })
                            .build());
                }
            }
        }

        ConfigCategory about = builder.getOrCreateCategory(Component.literal("关于"));
        about.addEntry(builder.entryBuilder()
                .startTextDescription(Component.literal("配置保存于 config/minecraft_love_you.json；关闭任意模块立即生效。主动功能键位请在原版“按键绑定”中修改。"))
                .build());

        builder.setSavingRunnable(WlyConfigData::save);
        return builder.build();
    }
}