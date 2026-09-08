package com.minecraftloveyou.client;

import com.minecraftloveyou.core.WlyCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** /wly keys list 的实际实现（客户端专用）。 */
public class KeyListCommand {

    private KeyListCommand() {
    }

    public static int list(CommandSourceStack src) {
        src.sendSuccess(() -> WlyCommands.prefix().append(Component.translatable("wly.command.keys.header")), false);
        for (var e : WlyKeys.all().entrySet()) {
            final String id = e.getKey();
            final String key = e.getValue().getTranslatedKeyMessage().getString();
            src.sendSuccess(() -> Component.literal(" - ").append(id).append(Component.literal(": ")).append(key), false);
        }
        return 1;
    }
}
