package com.minecraftloveyou.module.enchantreveal;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.core.WlyNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;

/**
 * 附魔台显示全部潜在附魔（enchant-reveal）：
 * 服务端在附魔台重算选项时把三档的全部候选列表（附魔名+等级）S2C 发给客户端，
 * 客户端 EnchantmentScreen 悬停行时把候选列表并入 tooltip。
 */
public class EnchantRevealModule implements WlyModule {

    public static final String ID = "enchant-reveal";

    public static void register() {
        ModuleManager.register(new EnchantRevealModule());
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
        // 数据采集在 mixin/EnchantmentMenuSlotsMixin；tooltip 在 client mixin
    }

    /** 服务端：把候选列表发给打开台子的玩家（mixin 调用）。 */
    public static void sendOffers(ServerPlayer player, java.util.List<java.util.List<String>> offers) {
        WlyNet.sendEnchantReveal(player, offers);
    }
}