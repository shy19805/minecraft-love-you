package com.minecraftloveyou.client;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.client.compat.XaeroCompat;
import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.graves.GravesModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * 墓碑路点：客户端每 10 tick 扫玩家周围 16 格墓碑方块，
 * 新出现的墓碑 → 调用 Xaero 联动加"墓碑"路点（已记录的不再重复）。
 */
public class GraveWaypointScanner {

    /**
     * 已登记过的墓碑（避免重复加路点）。
     * 必须是**有界**的：早期版本只增不减，长时间游玩/反复换世界会一直涨。
     * 到上限整体清空即可 —— 清空后最坏情况是给同一座墓碑再加一次路点，
     * 而 Xaero 同名同坐标的路点不会重复显示，代价可忽略。
     */
    private static final int MAX_KNOWN = 512;
    private static final Set<String> KNOWN = new HashSet<>();
    private static int tick = 0;
    private static boolean wasDead = false;
    private static double lastAliveX, lastAliveY, lastAliveZ;

    private GraveWaypointScanner() {
    }

    public static void register() {
        // 退出世界/换服务器时清空：不同存档的坐标完全可能撞车
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    KNOWN.clear();
                    wasDead = false;
                    tick = 0;
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WlyModule.on(GravesModule.ID) || !XaeroCompat.isXaeroPresent()) {
                return;
            }
            tick++;
            if (tick % 10 != 0) {
                return;
            }
            if (client.player == null || client.level == null) {
                return;
            }
            // 主通道：死亡瞬间在死亡位置直接加路点（零扫描、最精准）
            var p0 = client.player;
            if (p0.isDeadOrDying()) {
                if (!wasDead) {
                    wasDead = true;
                    XaeroCompat.addGraveWaypoint(
                            (int) Math.floor(lastAliveX), (int) Math.floor(lastAliveY), (int) Math.floor(lastAliveZ),
                            net.minecraft.network.chat.Component.translatable("wly.ui.waypoint.grave").getString());
                }
                return;
            }
            wasDead = false;
            lastAliveX = p0.getX();
            lastAliveY = p0.getY();
            lastAliveZ = p0.getZ();
            BlockPos base = p0.blockPosition();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos p = base.offset(dx, dy, dz);
                        if (client.level.getBlockState(p).getBlock() == GravesModule.GRAVE_BLOCK) {
                            String key = BuiltInRegistries.BLOCK.getKey(GravesModule.GRAVE_BLOCK)
                                    + ":" + p.getX() + "," + p.getY() + "," + p.getZ() + ":" + client.level.dimension().identifier();
                            if (KNOWN.size() >= MAX_KNOWN) {
                                KNOWN.clear();
                            }
                            if (KNOWN.add(key)) {
                                boolean ok = XaeroCompat.addGraveWaypoint(p.getX(), p.getY(), p.getZ(),
                                    net.minecraft.network.chat.Component.translatable("wly.ui.waypoint.grave").getString());
                                if (ok) {
                                    WorldLovesYouMod.LOGGER.info("[世界爱你] 已为墓碑 {} 添加 Xaero 路点", p.toShortString());
                                } else {
                                    KNOWN.remove(key); // 失败重试
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}