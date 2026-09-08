package com.minecraftloveyou.mixin;

import com.minecraftloveyou.module.enchantreveal.EnchantRevealModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 附魔台重算后（slotsChanged 尾部）把三档的全部候选发往客户端（enchant-reveal）。
 * getEnchantmentList 为 private → 通过 Mixin Invoker 暴露。
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuSlotsMixin {

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void wly$sendReveal(net.minecraft.world.Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (!(menu.slots.isEmpty())) {
            // 触发者上下文：从 menu 拿不到 player → 由 broadcastChanges 已同步；此处遍历 server 玩家打开的该菜单
            var server = com.minecraftloveyou.core.WlyServerRef.current();
            if (server == null) {
                return;
            }
            for (var player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu != menu) {
                    continue;
                }
                List<List<String>> offers = new ArrayList<>();
                var access = levelAccess(player);
                for (int ix = 0; ix < 3; ix++) {
                    List<String> names = new ArrayList<>();
                    try {
                        var list = ((EnchantmentMenuInvoker) menu).minecraftloveyou$getEnchantmentList(
                                access, menu.getSlot(0).getItem(), ix, menu.costs[ix]);
                        for (EnchantmentInstance inst : list) {
                            names.add(net.minecraft.world.item.enchantment.Enchantment.getFullname(inst.enchantment(), inst.level()).getString());
                        }
                    } catch (Exception ignored) {
                    }
                    offers.add(names);
                }
                EnchantRevealModule.sendOffers(player, offers);
            }
        }
    }

    private static net.minecraft.core.RegistryAccess levelAccess(ServerPlayer player) {
        return player.level().registryAccess();
    }
}
