package com.minecraftloveyou.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** AbstractVillager protected 方法 Invoker（visible-traders 用）。 */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerInvoker {

    @Invoker("addOffersFromTradeSet")
    void minecraftloveyou$invokeAddOffers(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> tradeSet);
}
