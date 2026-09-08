package com.minecraftloveyou.module.bigstacks;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 大堆叠（big-stacks）：堆叠上限 64 → 999。 */
public class BigStacksModule implements WlyModule {

    public static final String ID = "big-stacks";
    public static int maxStack = 999;

    public static void register() {
        ModuleManager.register(new BigStacksModule());
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
        // /bigstacktest <数量>：调试命令（服务端改主手数量，用于验证大堆叠）
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, reg, env) ->
                dispatcher.register(net.minecraft.commands.Commands.literal("bigstacktest")
                        .then(net.minecraft.commands.Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9_999_999))
                                .executes(c -> {
                                    var player = c.getSource().getPlayerOrException();
                                    var held = player.getMainHandItem();
                                    if (held.isEmpty()) {
                                        player.sendSystemMessage(com.minecraftloveyou.core.WlyCommands.prefix()
                                                .append(net.minecraft.network.chat.Component.translatable("wly.msg.big_stacks.need_item")));
                                        return 0;
                                    }
                                    held.setCount(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "count"));
                                    player.sendSystemMessage(com.minecraftloveyou.core.WlyCommands.prefix()
                                            .append(net.minecraft.network.chat.Component.translatable(
                                                    "wly.msg.big_stacks.set_count", held.getCount(), held.getMaxStackSize())));
                                    return 1;
                                }))));
    }
}
