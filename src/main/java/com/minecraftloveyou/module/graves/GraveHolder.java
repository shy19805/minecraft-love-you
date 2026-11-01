package com.minecraftloveyou.module.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死亡暂存：die 之前摘走的物品与经验（每玩家一份）。
 *
 * <p><b>为什么必须带「维度 + 坐标」</b>：暂存里的物品是玩家**全部家当**。
 * 一旦 {@code AFTER_DEATH} 没走到（模块被关、服务端中途停止、玩家在死亡界面掉线），
 * 这份快照就再也没有人去取 —— 既是内存泄漏，更是**物品永久丢失**。
 * 所以快照必须记住掉落位置，让兜底逻辑能把东西原样掉回世界。
 */
public final class GraveHolder {

    /** 快照存活上限：超过这个时间还没被取走，视为异常，强制掉落。 */
    private static final long MAX_PENDING_MS = 10 * 60_000L;
    /** 并发上限：正常情况下最多 = 在线玩家数；超过说明有泄漏，强制清理。 */
    private static final int MAX_PENDING = 64;

    public record Snapshot(List<ItemStack> items, int xp, String owner,
                           ResourceKey<Level> dimension, double x, double y, double z, long capturedAt) {
    }

    private static final Map<UUID, Snapshot> PENDING = new ConcurrentHashMap<>();

    private GraveHolder() {
    }

    /** die HEAD 调用：摘走全部物品与经验。 */
    public static void capture(ServerPlayer player) {
        List<ItemStack> all = new ArrayList<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                all.add(s.copy());
            }
        }
        inv.clearContent();
        Snapshot prev = PENDING.put(player.getUUID(), new Snapshot(all, player.totalExperience,
                player.getName().getString(), player.level().dimension(),
                player.getX(), player.getY(), player.getZ(), System.currentTimeMillis()));
        if (prev != null) {
            // 上一次还没被取走就被覆盖：先把旧东西掉回世界，绝不静默吞掉
            drop(player.level(), prev);
        }
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.totalExperience = 0;
        com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[世界爱你] {} 死亡：背包+经验已暂存（{}格）",
                player.getName().getString(), all.size());
    }

    public static Snapshot take(UUID uuid) {
        return PENDING.remove(uuid);
    }

    /** 玩家掉线时调用：还没取走的快照立刻掉回世界（否则物品随快照一起消失）。 */
    public static void flush(UUID uuid, net.minecraft.server.MinecraftServer server) {
        Snapshot s = PENDING.remove(uuid);
        if (s == null) {
            return;
        }
        ServerLevel level = server.getLevel(s.dimension());
        if (level == null) {
            com.minecraftloveyou.WorldLovesYouMod.LOGGER.warn(
                    "[世界爱你] {} 掉线时仍有死亡暂存，但维度 {} 不可用，物品已丢失（{}格）",
                    s.owner(), s.dimension().identifier(), s.items().size());
            return;
        }
        drop(level, s);
    }

    /** 服务端停止前调用：把还挂着的快照全部掉回世界，并清空（杜绝跨存档残留）。 */
    public static void flushAll(net.minecraft.server.MinecraftServer server) {
        for (UUID id : new ArrayList<>(PENDING.keySet())) {
            flush(id, server);
        }
        PENDING.clear();
    }

    /** 兜底：把快照内容原样掉回指定世界。 */
    public static void drop(ServerLevel level, Snapshot s) {
        if (level == null) {
            return;
        }
        for (ItemStack st : s.items()) {
            if (!st.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, s.x(), s.y() + 0.5, s.z(), st);
            }
        }
        if (s.xp() > 0) {
            ExperienceOrb.award(level, new net.minecraft.world.phys.Vec3(s.x(), s.y() + 0.5, s.z()), s.xp());
        }
        com.minecraftloveyou.WorldLovesYouMod.LOGGER.info(
                "[世界爱你] 死亡暂存兜底：{} 的 {} 格物品 + {} 经验已掉回世界（{}）",
                s.owner(), s.items().size(), s.xp(), level.dimension().identifier());
    }

    /** 定期清理：超过 {@link #MAX_PENDING_MS} 还没被取走的快照强制掉落。 */
    public static void sweep(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (PENDING.size() > MAX_PENDING) {
            com.minecraftloveyou.WorldLovesYouMod.LOGGER.warn(
                    "[世界爱你] 死亡暂存异常堆积（{} 条，上限 {}），强制清理", PENDING.size(), MAX_PENDING);
            flushAll(server);
            return;
        }
        for (Map.Entry<UUID, Snapshot> e : PENDING.entrySet()) {
            if (now - e.getValue().capturedAt() > MAX_PENDING_MS) {
                Snapshot s = PENDING.remove(e.getKey());
                if (s != null) {
                    drop(server.getLevel(s.dimension()), s);
                }
            }
        }
    }

    /** 便捷：从 BlockPos 造快照位置（供外部构造测试用）。 */
    public static Snapshot at(BlockPos pos, List<ItemStack> items, int xp, String owner, ResourceKey<Level> dim) {
        return new Snapshot(items, xp, owner, dim, pos.getX(), pos.getY(), pos.getZ(), System.currentTimeMillis());
    }

    /** 当前挂起的快照数量（自检/诊断用）。 */
    public static int pendingCount() {
        return PENDING.size();
    }
}
