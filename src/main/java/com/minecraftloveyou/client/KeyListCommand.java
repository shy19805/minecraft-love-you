package com.minecraftloveyou.client;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** /wly keys list 的实际实现（客户端专用）。 */
public class KeyListCommand {

    private KeyListCommand() {
    }

    public static int list(CommandSourceStack src) {
        var sb = new StringBuilder("[世界爱你] 键位：\n");
        for (var e : WlyKeys.all().entrySet()) {
            sb.append(" - ").append(e.getKey()).append("：")
              .append(e.getValue().getTranslatedKeyMessage().getString()).append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}