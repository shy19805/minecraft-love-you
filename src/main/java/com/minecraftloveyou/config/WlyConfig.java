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
        put("powershot", "wly.category.combat");
        put("trajectory", "wly.category.combat");
        put("friendly-fire", "wly.category.combat");
        put("infinity-arrows", "wly.category.combat");
        put("climbing", "wly.category.combat");
        put("eztweak-creeper-no-explode", "wly.category.combat");
        put("eztweak-ghast-no-grief", "wly.category.combat");
        put("eztweak-enderman-grief", "wly.category.combat");
        put("eztweak-farmland-protect", "wly.category.combat");
        put("rendi", "wly.category.combat");
        put("elb", "wly.category.combat");
        put("anvil-master", "wly.category.combat");
        put("anvil-no-limit", "wly.category.combat");
        put("anvil-repair", "wly.category.combat");
        put("reroll", "wly.category.combat");
        put("enchant-reveal", "wly.category.combat");
        put("enchant-tweak", "wly.category.combat");
        put("nonconflict-keys", "wly.category.combat");
        // ── 农业与生存 ──
        put("harvest", "wly.category.farming");
        put("eat-full", "wly.category.farming");
        put("eat-fast", "wly.category.farming");
        put("graves", "wly.category.farming");
        put("maxhealth-fix", "wly.category.farming");
        put("mend-hold", "wly.category.farming");
        put("beacon-fill", "wly.category.farming");
        put("vine-extend", "wly.category.farming");
        put("open-water", "wly.category.farming");
        put("snow-cap", "wly.category.farming");
        put("smooth-bedrock", "wly.category.farming");
        put("offline-catchup", "wly.category.farming");
        // ── 物品与便利 ──
        put("big-stacks", "wly.category.items");
        put("sort", "wly.category.items");
        put("clickthrough", "wly.category.items");
        put("splash-milk", "wly.category.items");
        put("ruler", "wly.category.items");
        put("no-exp-warn", "wly.category.items");
        put("visible-traders", "wly.category.items");
        put("smart-tooltip", "wly.category.items");
        put("trade-display", "wly.category.items");
        put("ctm", "wly.category.items");
        // ── 生物与探索 ──
        put("tame-safety", "wly.category.mobs");
        put("emerald-lure", "wly.category.mobs");
        put("spawner-plus", "wly.category.mobs");
    }};

    private static final String GROUP_OTHER = "wly.category.other";

    private static String groupOf(String id) {
        return GROUP_RULES.getOrDefault(id, GROUP_OTHER);
    }

    /** 构建 Cloth Config 界面（Mod Menu 入口调用）。 */
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("wly.modmenu.title"));

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
            ConfigCategory cat = builder.getOrCreateCategory(Component.translatable(entry.getKey()));
            for (var m : entry.getValue()) {
                if (m.hideToggle()) {
                    continue; // 静态资源类功能（如 ctm）：开关无效，不显示
                }
                String id = m.id();
                boolean cur = WlyConfigData.isStored(id)
                        ? WlyConfigData.moduleEnabled(id)
                        : m.enabledByDefault();
                cat.addEntry(builder.entryBuilder()
                        .startBooleanToggle(Component.translatable(m.displayName())
                                        .append(Component.literal(" (" + id + ")")), cur)
                        .setDefaultValue(m.enabledByDefault())
                        // 每个模块显示自己的功效说明（wly.module.<id>.desc），不再是统一的「关闭后完全失效」
                        .setTooltip(Component.translatable("wly.module." + id + ".desc"))
                        .setSaveConsumer(v -> WlyConfigData.put(id, v))
                        .build());

                // 联动配置项：所有箭配无限开关下方紧邻「免箭矢库存」子开关
                if (id.equals("infinity-arrows")) {
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.translatable("wly.config.infinity_arrows.free_arrow"),
                                    com.minecraftloveyou.module.infarrows.InfArrowsModule.freeArrowWithoutStock)
                            .setDefaultValue(true)
                            .setTooltip(Component.translatable("wly.config.infinity_arrows.free_arrow.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.infarrows.InfArrowsModule.freeArrowWithoutStock = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：力量射击开关下方紧邻初速数值 + 伤害开关/上限
                if (id.equals("powershot")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.translatable("wly.config.powershot.velocity"),
                                    (float) com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity)
                            .setMin(0.0F).setMax(10.0F)
                            .setDefaultValue(2.5F)
                            .setTooltip(Component.translatable("wly.config.powershot.velocity.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.translatable("wly.config.powershot.damage"),
                                    com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled)
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("wly.config.powershot.damage.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.powershot.damage_max"),
                                    com.minecraftloveyou.module.powershot.PowershotModule.damageMax)
                            .setMin(0).setMax(50)
                            .setDefaultValue(50)
                            .setTooltip(Component.translatable("wly.config.powershot.damage_max.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.powershot.PowershotModule.damageMax = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：铁砧修复开关下方紧邻每档消耗铁锭数
                if (id.equals("anvil-repair")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.anvil_repair.cost"),
                                    com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairCost)
                            .setMin(1).setMax(31)
                            .setDefaultValue(3)
                            .setTooltip(Component.translatable("wly.config.anvil_repair.cost.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairCost = Math.max(1, v);
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：大堆叠开关下方紧邻堆叠上限
                if (id.equals("big-stacks")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.big_stacks.max_stack"), com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack)
                            .setMin(1)
                            .setMax(9_999_999)
                            .setDefaultValue(999)
                            .setTooltip(Component.translatable("wly.config.big_stacks.max_stack.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack = Math.max(1, v);
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：满饱仍可吃开关下方紧邻饱和度上限
                if (id.equals("eat-full")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.eat_full.saturation_cap"), com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap)
                            .setMin(0)
                            .setMax(2_100_000_000)
                            .setDefaultValue(100)
                            .setTooltip(Component.translatable("wly.config.eat_full.saturation_cap.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：右键收获并补种开关下方紧邻三个连锁参数
                if (id.equals("harvest")) {
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.harvest.chain_limit"), com.minecraftloveyou.module.harvest.HarvestModule.chainLimit)
                            .setMin(1).setMax(4096)
                            .setDefaultValue(64)
                            .setTooltip(Component.translatable("wly.config.harvest.chain_limit.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.chainLimit = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.translatable("wly.config.harvest.same_species"), com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies)
                            .setDefaultValue(true)
                            .setTooltip(Component.translatable("wly.config.harvest.same_species.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startIntField(Component.translatable("wly.config.harvest.plant_limit"), com.minecraftloveyou.module.harvest.HarvestModule.plantLimit)
                            .setMin(1).setMax(4096)
                            .setDefaultValue(64)
                            .setTooltip(Component.translatable("wly.config.harvest.plant_limit.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.harvest.HarvestModule.plantLimit = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：快吃开关下方紧邻进食速度
                if (id.equals("eat-fast")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.translatable("wly.config.eat_fast.speed"), com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed)
                            .setMin(0.0F).setMax(2.0F)
                            .setDefaultValue(1.0F)
                            .setTooltip(Component.translatable("wly.config.eat_fast.speed.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed = v;
                                WlyConfigData.save();
                            })
                            .build());
                }

                // 联动配置项：更好的攀爬开关下方紧邻攀爬速度 + 下滑
                if (id.equals("climbing")) {
                    cat.addEntry(builder.entryBuilder()
                            .startFloatField(Component.translatable("wly.config.climbing.speed"), (float) com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed)
                            .setMin(0.0F).setMax(10.0F)
                            .setDefaultValue(2.0F)
                            .setTooltip(Component.translatable("wly.config.climbing.speed.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed = v;
                                WlyConfigData.save();
                            })
                            .build());
                    cat.addEntry(builder.entryBuilder()
                            .startBooleanToggle(Component.translatable("wly.config.climbing.down_boost"), com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost)
                            .setDefaultValue(true)
                            .setTooltip(Component.translatable("wly.config.climbing.down_boost.tooltip"))
                            .setSaveConsumer(v -> {
                                com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost = v;
                                WlyConfigData.save();
                            })
                            .build());
                }
            }
        }

        ConfigCategory about = builder.getOrCreateCategory(Component.translatable("wly.category.about"));
        about.addEntry(builder.entryBuilder()
                .startTextDescription(Component.translatable("wly.config.about.desc"))
                .build());

        builder.setSavingRunnable(WlyConfigData::save);
        return builder.build();
    }
}