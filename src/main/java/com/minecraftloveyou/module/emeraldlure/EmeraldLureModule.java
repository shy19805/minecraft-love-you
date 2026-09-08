package com.minecraftloveyou.module.emeraldlure;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 绿宝石吸引村民：手持绿宝石/绿宝石块，8 格内村民跟随（2 格内停下）。 */
public class EmeraldLureModule implements WlyModule {

    public static final String ID = "emerald-lure";
    private static int tick = 0;

    public static void register() {
        ModuleManager.register(new EmeraldLureModule());
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
    public void init(ModuleContext ctx) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WlyModule.on(ID)) {
                return;
            }
            tick++;
            if (tick % 10 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ItemStack held = player.getMainHandItem();
                boolean lure = held.is(Items.EMERALD) || held.is(Items.EMERALD_BLOCK);
                if (!lure) {
                    continue;
                }
                List<AbstractVillager> villagers = player.level().getEntitiesOfClass(
                        AbstractVillager.class, player.getBoundingBox().inflate(8, 4, 8));
                for (var v : villagers) {
                    if (v.distanceToSqr(player) > 4) {
                        v.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.1);
                    }
                }
            }
        });
    }
}
