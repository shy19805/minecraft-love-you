package com.minecraftloveyou.client;

import com.minecraftloveyou.core.WlyNet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** 客户端：周期性同步连锁键状态（每 5 tick 重发，幂等、抗丢包/竞态）。 */
public class ChainKeySender {

    private static boolean lastHeld = false;
    private static int tick = 0;

    private ChainKeySender() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean held = WlyKeys.active("harvest");
            tick++;
            // 状态变化立即发；稳定保持时仅每 100 tick 心跳一次（抗掉线，网络开销最小化）
            boolean resend = tick % 100 == 0;
            if (held != lastHeld || (held && resend)) {
                lastHeld = held;
                if (client.getConnection() != null && (held || !resend)) {
                    ClientPlayNetworking.send(new WlyNet.ChainKeyPayload(held));
                }
            }
        });
    }
}