package com.minecraftloveyou.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

/** enchant-reveal 客户端缓存：三档候选列表（最新一次台子重算）。 */
public final class EnchantRevealCache {

    private static volatile List<List<String>> offers = List.of(List.of(), List.of(), List.of());

    private EnchantRevealCache() {
    }

    private static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<com.minecraftloveyou.core.WlyNet.RevealPayload> TYPE =
            com.minecraftloveyou.core.WlyNet.RevealPayload.TYPE;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> offers = payload.offers());
    }

    public static List<List<String>> offers() {
        return offers;
    }
}