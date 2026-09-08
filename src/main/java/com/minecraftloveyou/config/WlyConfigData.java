package com.minecraftloveyou.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minecraftloveyou.WorldLovesYouMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置数据层（纯 Gson，零客户端依赖）：专服/双端安全。
 * WlyConfig（GUI）与此类通信；服务端只允许触碰本类。
 */
public final class WlyConfigData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Boolean> STORE = new LinkedHashMap<>();
    private static boolean loaded = false;

    private WlyConfigData() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("minecraft_love_you.json");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path p = configPath();
        STORE.clear();
        if (Files.exists(p)) {
            try {
                Map<String, Object> raw = GSON.fromJson(Files.readString(p), new TypeToken<Map<String, Object>>() {}.getType());
                if (raw != null) {
                    Float cap = readNumber(raw, "__eatFullCap");
                    if (cap != null) {
                        com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap = cap.intValue();
                    }
                    Float eatSpeed = readNumber(raw, "__eatFastSpeed");
                    if (eatSpeed != null) {
                        com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed = eatSpeed;
                    }
                    Integer chainLimit = readNumber(raw, "__harvestChainLimit") == null ? null : readNumber(raw, "__harvestChainLimit").intValue();
                    if (chainLimit != null) {
                        com.minecraftloveyou.module.harvest.HarvestModule.chainLimit = chainLimit;
                    }
                    Boolean sameSpecies = readBool(raw, "__harvestSameSpecies");
                    if (sameSpecies != null) {
                        com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies = sameSpecies;
                    }
                    Integer plantLimit = readNumber(raw, "__harvestPlantLimit") == null ? null : readNumber(raw, "__harvestPlantLimit").intValue();
                    if (plantLimit != null) {
                        com.minecraftloveyou.module.harvest.HarvestModule.plantLimit = plantLimit;
                    }
                    Float climbSpeed = readNumber(raw, "__climbSpeed");
                    if (climbSpeed != null) {
                        com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed = climbSpeed;
                    }
                    Boolean downLook = readBool(raw, "__climbDownLook");
                    if (downLook != null) {
                        com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost = downLook;
                    }
                    Float powVel = readNumber(raw, "__powerVelocity");
                    if (powVel != null) {
                        com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = powVel;
                    }
                    Boolean powDmg = readBool(raw, "__damageEnabled");
                    if (powDmg != null) {
                        com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = powDmg;
                    }
                    Integer powDmgMax = readNumber(raw, "__damageMax") == null ? null : readNumber(raw, "__damageMax").intValue();
                    if (powDmgMax != null) {
                        com.minecraftloveyou.module.powershot.PowershotModule.damageMax = powDmgMax;
                    }
                    // 大堆叠上限：必须恢复（此前只 remove 未 restore，导致重启回 999）
                    Integer bigStack = readNumber(raw, "__bigStack") == null ? null : readNumber(raw, "__bigStack").intValue();
                    if (bigStack != null) {
                        com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack = bigStack;
                    }
                    raw.remove("__eatFullCap");
                    raw.remove("__bigStack");
                    raw.remove("__eatFastSpeed");
                    raw.remove("__harvestChainLimit");
                    raw.remove("__harvestSameSpecies");
                    raw.remove("__harvestPlantLimit");
                    raw.remove("__climbSpeed");
                    raw.remove("__climbDownLook");
                    raw.remove("__powerVelocity");
                    raw.remove("__damageEnabled");
                    raw.remove("__damageMax");
                    for (Map.Entry<String, Object> e : raw.entrySet()) {
                        if (e.getValue() instanceof Boolean b) {
                            STORE.put(e.getKey(), b);
                        }
                    }
                }
            } catch (Exception e) {
                WorldLovesYouMod.LOGGER.warn("[世界爱你] 配置文件读取失败，使用默认值：{}", e.toString());
            }
        }
    }

    private static Float readNumber(Map<String, Object> raw, String key) {
        if (raw != null && raw.get(key) instanceof Number n) {
            return n.floatValue();
        }
        return null;
    }

    private static Boolean readBool(Map<String, Object> raw, String key) {
        if (raw != null && raw.get(key) instanceof Boolean b) {
            return b;
        }
        return null;
    }

    /** 缓存刷新。 */
    public static void reload() {
        loaded = false;
        ensureLoaded();
    }

    public static boolean moduleEnabled(String id) {
        ensureLoaded();
        boolean dflt = true;
        var m = com.minecraftloveyou.core.ModuleManager.modules().get(id);
        if (m != null) {
            dflt = m.enabledByDefault();
        }
        return STORE.getOrDefault(id, dflt);
    }

    public static boolean isStored(String id) {
        ensureLoaded();
        return STORE.containsKey(id);
    }

    public static boolean defaultValueOf(String id) {
        return true; // 全部功能默认开
    }

    public static void put(String id, boolean value) {
        STORE.put(id, value);
        saveFile();
    }

    public static Map<String, Boolean> storeView() {
        ensureLoaded();
        return STORE;
    }

    private static void saveFile() {
        try {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.putAll(STORE);
            out.put("__eatFullCap", com.minecraftloveyou.module.eatfull.EatFullModule.saturationCap);
            out.put("__bigStack", com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack);
            out.put("__eatFastSpeed", com.minecraftloveyou.module.eatfast.EatFastModule.eatSpeed);
            out.put("__harvestChainLimit", com.minecraftloveyou.module.harvest.HarvestModule.chainLimit);
            out.put("__harvestSameSpecies", com.minecraftloveyou.module.harvest.HarvestModule.sameSpecies);
            out.put("__harvestPlantLimit", com.minecraftloveyou.module.harvest.HarvestModule.plantLimit);
            out.put("__climbSpeed", com.minecraftloveyou.module.climbing.ClimbingModule.climbSpeed);
            out.put("__climbDownLook", com.minecraftloveyou.module.climbing.ClimbingModule.allowDownLookBoost);
            out.put("__powerVelocity", com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity);
            out.put("__damageEnabled", com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled);
            out.put("__damageMax", com.minecraftloveyou.module.powershot.PowershotModule.damageMax);
            Files.writeString(configPath(), GSON.toJson(out));
        } catch (IOException e) {
            WorldLovesYouMod.LOGGER.error("[世界爱你] 配置写入失败：{}", e.toString());
        }
    }

    public static void save() {
        saveFile();
    }
}