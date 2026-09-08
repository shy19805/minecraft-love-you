package com.minecraftloveyou.core;

import net.minecraft.server.MinecraftServer;

/** 当前运行中的 MinecraftServer 引用（ServerLifecycleEvents 维护）。 */
public final class WlyServerRef {

    private static volatile MinecraftServer server;

    private WlyServerRef() {
    }

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }

    public static MinecraftServer current() {
        return server;
    }
}
