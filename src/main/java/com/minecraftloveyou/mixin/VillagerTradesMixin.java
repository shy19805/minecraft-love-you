package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 显示村民全部交易（visible-traders）：updateTrades 尾部把村民职业等级升至 5，
 * 全部 tier 交易立即生成并显示（新交易全部可买）。
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void wly$unlockAllTrades(ServerLevel level, CallbackInfo ci) {
        if (!WlyModule.on("visible-traders")) {
            return;
        }
        Villager self = (Villager) (Object) this;
        var data = self.getVillagerData();
        int cur = data.level();
        var offers = self.getOffers();
        int before = offers.size();
        for (int lvl = cur + 1; lvl <= 5; lvl++) {
            var trades = data.profession().value().getTrades(lvl);
            if (trades != null) {
                ((AbstractVillagerInvoker) self).minecraftloveyou$invokeAddOffers(level, offers, trades);
            }
        }
        // 新增部分（低级村民看不到的高级交易）标记为缺货灰显；村民升级后 updateTrades 会重生成该档 → 正常可买
        for (int i = before; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            while (!offer.isOutOfStock()) {
                offer.increaseUses();
            }
        }
    }
}