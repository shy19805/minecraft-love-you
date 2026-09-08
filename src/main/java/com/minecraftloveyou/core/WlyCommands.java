package com.minecraftloveyou.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

/** /wly 巡检指令：列出模块状态；键位列表由客户端侧另行注册。 */
public class WlyCommands {

    private WlyCommands() {
    }

    /** client=true 时额外注册 /wly keys list（仅客户端可用）。 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean client) {
        var root = literal("wly")
                .then(literal("list").executes(ctx -> listModules(ctx.getSource())));
        if (client) {
            root = root.then(literal("keys")
                    .then(literal("list").executes(ctx -> com.minecraftloveyou.client.KeyListCommand.list(ctx.getSource()))));
        }
        dispatcher.register(root);
    }

    private static int listModules(CommandSourceStack src) {
        var sb = new StringBuilder("[我的世界爱你] 模块状态：\n");
        for (var m : ModuleManager.modules().values()) {
            sb.append(" - ").append(m.displayName()).append("（").append(m.id()).append("）：")
              .append(ModuleManager.enabled(m.id()) ? "§a启用" : "§c禁用").append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}