package com.minecraftloveyou.module.ruler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局游戏规则锁定（26.2 新版 gamerules API）：
 * config/minecraft_love_you/ruler.json = {"keep_inventory": true, "random_tick_speed": 3, ...}
 * 规则名使用 26.2 snake_case；SERVER_STARTED 与每次玩家进入都会应用。
 */
public class RulerModule implements WlyModule {

    public static final String ID = "ruler";
    private static final Gson GSON = new Gson();

    public static void register() {
        ModuleManager.register(new RulerModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "全局规则锁定";
    }

    @Override
    public void init(ModuleContext ctx) {
        if (!on()) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!on()) {
                return;
            }
            applyToServer(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!on()) {
                return;
            }
            apply(handler.player.level());
        });
    }

    private static Path configDir() {
        Path p = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("minecraft_love_you");
        try {
            Files.createDirectories(p);
        } catch (Exception ignored) {
        }
        return p;
    }

    private static JsonObject loadRules() {
        Path p = configDir().resolve("ruler.json");
        if (!Files.exists(p)) {
            return new JsonObject();
        }
        try {
            return GSON.fromJson(Files.readString(p), JsonObject.class);
        } catch (Exception e) {
            WorldLovesYouMod.LOGGER.warn("[世界爱你] ruler.json 解析失败：{}", e.toString());
            return new JsonObject();
        }
    }

    private static void applyToServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            apply(level);
        }
    }

    /** 遍历已注册 gamerules，按 json 键名（snake_case）命中并设置。 */
    @SuppressWarnings("unchecked")
    private static void apply(ServerLevel level) {
        GameRules rules = level.getGameRules();
        JsonObject cfg = loadRules();
        if (cfg == null || cfg.isEmpty()) {
            return;
        }
        List<GameRule<?>> all = new ArrayList<>();
        net.minecraft.core.registries.BuiltInRegistries.GAME_RULE.stream().forEach(all::add);
        for (GameRule<?> rule : all) {
            String id = rule.id();
            if (!cfg.has(id)) {
                continue;
            }
            JsonElement el = cfg.get(id);
            try {
                setRule(rules, (GameRule<Object>) rule, el);
            } catch (Exception ex) {
                WorldLovesYouMod.LOGGER.warn("[世界爱你] 应用规则 {} 失败：{}", id, ex.toString());
            }
        }
    }

    private static void setRule(GameRules rules, GameRule<Object> rule, JsonElement el) {
        Object value;
        Class<?> vc = rule.valueClass();
        if (vc == Boolean.class) {
            value = booleanOf(el);
        } else if (vc == Integer.class) {
            value = intOf(el);
        } else {
            value = rule.deserialize(el.getAsString()).result().orElse(null);
        }
        if (value != null) {
            rules.set(rule, value, null);
        }
    }

    private static boolean booleanOf(JsonElement e) {
        if (e instanceof JsonPrimitive p && p.isBoolean()) {
            return p.getAsBoolean();
        }
        return Boolean.parseBoolean(e.getAsString());
    }

    private static int intOf(JsonElement e) {
        return e.getAsInt();
    }
}