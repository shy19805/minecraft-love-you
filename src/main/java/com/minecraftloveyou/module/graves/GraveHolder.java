package com.minecraftloveyou.module.graves;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 死亡暂存：die 之前摘走的物品与经验（每玩家一份）。 */
public final class GraveHolder {

    public record Snapshot(List<ItemStack> items, int xp, String owner) {
    }

    private static final java.util.Map<java.util.UUID, Snapshot> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

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
        PENDING.put(player.getUUID(), new Snapshot(all, player.totalExperience, player.getName().getString()));
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.totalExperience = 0;
        com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[世界爱你] {} 死亡：背包+经验已暂存（{}格）",
                player.getName().getString(), all.size());
    }

    public static Snapshot take(java.util.UUID uuid) {
        return PENDING.remove(uuid);
    }
}