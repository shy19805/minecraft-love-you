package com.minecraftloveyou.module.anvilmaster;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.RomanNumeral;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 附魔大师：
 * - 铁砧解除 40 级 too-expensive 上限（成本照付）
 * - 附魔等级显示罗马数字（超过原版 I~X 范围也显示罗马字）
 * - /enchant-clear：清除主手物品全部附魔（true=含诅咒，默认不含诅咒）
 */
public class AnvilMasterModule implements WlyModule {

    public static final String ID = "anvil-master";

    public static void register() {
        ModuleManager.register(new AnvilMasterModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "铁砧无上限与清附魔";
    }

    @Override
    public void init(ModuleContext ctx) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                dispatcher.register(literal("enchant-clear")
                        
                        .executes(ctx2 -> clear(ctx2.getSource().getPlayerOrException(), false))
                        .then(argument("includeCurse", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(ctx2 -> clear(ctx2.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx2, "includeCurse"))))));
    }

    private static int clear(ServerPlayer player, boolean includeCurse) {
        if (!WlyModule.on(ID)) {
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("[我的世界爱你] 手持空物品"), true);
            return 0;
        }
        var ench = stack.get(DataComponents.ENCHANTMENTS);
        if (ench == null || ench.isEmpty()) {
            player.sendSystemMessage(Component.literal("[我的世界爱你] 手持物品没有附魔"), true);
            return 0;
        }
        var kept = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(ench);
        kept.removeIf(h -> includeCurse || !h.is(EnchantmentTags.CURSE));
        stack.set(DataComponents.ENCHANTMENTS, kept.toImmutable());
        stack.set(DataComponents.REPAIR_COST, 0);
        player.sendSystemMessage(Component.literal("[我的世界爱你] 已清除附魔"), true);
        return 1;
    }
}