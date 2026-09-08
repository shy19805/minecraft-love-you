package com.minecraftloveyou.core;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.mixin.EnchantmentMenuAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;

/** 附魔菜单内部访问桥（供 reroll 强制重算）。 */
public final class WlyNetBridge {

    private WlyNetBridge() {
    }

    /** 改 menu 的种子 DataSlot 并触发重算（通过 Mixin accessor）。 */
    public static void rerollEnchantmentMenu(ServerPlayer player, EnchantmentMenu menu, int newSeed) {
        if (menu instanceof EnchantmentMenuAccessor acc) {
            acc.minecraftloveyou$getSeedSlot().set(newSeed);
            menu.slotsChanged(acc.minecraftloveyou$getEnchantSlots());
            WorldLovesYouMod.LOGGER.info("[我的世界爱你][reroll] 附魔选项已重掷（seed={}）", newSeed);
        }
    }
}