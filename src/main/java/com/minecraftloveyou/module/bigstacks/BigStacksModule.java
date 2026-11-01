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
        // 需要 OP 2 级 + 模块处于启用状态（否则等于给了所有人一条任意改物品数量的作弊指令）
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, reg, env) ->
                dispatcher.register(net.minecraft.commands.Commands.literal("bigstacktest")
                        // 26.x 权限系统：CommandSourceStack 没有 hasPermission(int)，
                        // 走 permissions() + Permissions 常量（两版都有）
                        .requires(src -> src.permissions().hasPermission(
                                net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(net.minecraft.commands.Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9_999_999))
                                .executes(c -> {
                                    if (!on()) {
                                        return 0;
                                    }
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
