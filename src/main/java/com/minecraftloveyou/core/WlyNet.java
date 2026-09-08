package com.minecraftloveyou.core;

import com.minecraftloveyou.WorldLovesYouMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义网络：C2S 连锁键状态 + S2C 附魔台全部候选（enchant-reveal）。
 */
public class WlyNet {

    /** C2S：连锁键状态。 */
    public record ChainKeyPayload(boolean held) implements CustomPacketPayload {
        public static final Type<ChainKeyPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "chain_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChainKeyPayload> CODEC =
                StreamCodec.of(
                        (buf, p) -> buf.writeBoolean(p.held()),
                        buf -> new ChainKeyPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S2C：三档附魔的全部候选（已格式化字符串）。 */
    public record RevealPayload(List<List<String>> offers) implements CustomPacketPayload {
        public static final Type<RevealPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "enchant_reveal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RevealPayload> CODEC =
                StreamCodec.of(RevealPayload::writeReveal, RevealPayload::readReveal);

        private static void writeReveal(RegistryFriendlyByteBuf buf, RevealPayload p) {
            buf.writeVarInt(p.offers().size());
            for (List<String> row : p.offers()) {
                buf.writeVarInt(row.size());
                for (String str : row) {
                    buf.writeUtf(str, 256);
                }
            }
        }

        private static RevealPayload readReveal(RegistryFriendlyByteBuf buf) {
            int rows = buf.readVarInt();
            List<List<String>> offs = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                int n = buf.readVarInt();
                List<String> row = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    row.add(buf.readUtf(256));
                }
                offs.add(row);
            }
            return new RevealPayload(offs);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 服务端记录：玩家 → 是否按住连锁键。 */
    public static final Map<UUID, Boolean> CHAIN_HELD = new ConcurrentHashMap<>();

    public static boolean chainHeld(ServerPlayer player) {
        return CHAIN_HELD.getOrDefault(player.getUUID(), false);
    }

    /** 服务端发候选列表。 */
    public static void sendEnchantReveal(ServerPlayer player, List<List<String>> offers) {
        ServerPlayNetworking.send(player, new RevealPayload(offers));
    }

    public static void register() {
        WlyServerRef.register();
        PayloadTypeRegistry.serverboundPlay().register(ChainKeyPayload.TYPE, ChainKeyPayload.CODEC);
        // 刷新附魔请求（附魔台界面按键/按钮）
        PayloadTypeRegistry.serverboundPlay().register(
                com.minecraftloveyou.module.reroll.RerollModule.RerollRequest.TYPE,
                com.minecraftloveyou.module.reroll.RerollModule.RerollRequest.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                com.minecraftloveyou.module.reroll.RerollModule.RerollRequest.TYPE,
                (payload, context) -> com.minecraftloveyou.module.reroll.RerollModule.handleReroll(context.player()));
        // 一键整理请求
        PayloadTypeRegistry.serverboundPlay().register(com.minecraftloveyou.core.SortPayload.TYPE, com.minecraftloveyou.core.SortPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.minecraftloveyou.core.SortPayload.TYPE, (payload, context) -> {
            if (WlyModule.on("sort")) {
                SortPayload.handle(context.player());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ChainKeyPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            CHAIN_HELD.put(player.getUUID(), payload.held());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CHAIN_HELD.remove(handler.getPlayer().getUUID()));
        PayloadTypeRegistry.clientboundPlay().register(RevealPayload.TYPE, RevealPayload.CODEC);
        WorldLovesYouMod.LOGGER.info("[我的世界爱你] 网络通道注册完成（连锁键 + 附魔候选）");
    }
}