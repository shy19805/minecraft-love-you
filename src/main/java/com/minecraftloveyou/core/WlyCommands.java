package com.minecraftloveyou.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.minecraft.commands.Commands.literal;

/** /wly 巡检指令：列出模块状态；键位列表由客户端侧另行注册。 */
public class WlyCommands {

    private WlyCommands() {
    }

    /** 消息统一前缀：[模组名]（模组名走语言键，跟随客户端语言）。 */
    public static MutableComponent prefix() {
        return Component.literal("[").append(Component.translatable("wly.modmenu.title")).append(Component.literal("] "));
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
        src.sendSuccess(() -> prefix().append(Component.translatable("wly.command.header")), false);
        for (var m : ModuleManager.modules().values()) {
            final boolean on = ModuleManager.enabled(m.id());
            final String id = m.id();
            final String key = m.displayName();
            src.sendSuccess(() -> Component.literal(" - ")
                    .append(Component.translatable(key))
                    .append(Component.literal(" (" + id + "): "))
                    .append(Component.translatable(on ? "wly.command.on" : "wly.command.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        }
        return 1;
    }
}
