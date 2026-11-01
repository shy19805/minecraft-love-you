package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示村民全部交易（visible-traders）：updateTrades 尾部把村民职业**当前档以上**的交易
 * 也补进 offers 并标为缺货（客户端由 MerchantScreenStockMixin 画成不灰显），
 * 这样低级村民也能一眼看到该职业的全部交易内容（仅显示层，不改变实际可购性）。
 *
 * <p><b>为什么需要 wly$injected</b>：vanilla 在村民每次升级（{@code increaseMerchantCareer}）
 * 都会再调一次 {@code updateTrades}，而我们在 TAIL 无条件追加 cur+1..5 档 →
 * 每升一级就把 3/4/5 档再追加一遍，交易列表越堆越多（同一档出现 N 份）。
 * 所以每次注入前先把**上一轮注入的假交易**按引用摘掉。
 * {@code MerchantOffer} 没重写 equals → {@code removeAll} 是按引用比较，不会误删 vanilla 的交易。
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    /** 上一轮注入的假交易（按引用记住）。 */
    @Unique
    private List<MerchantOffer> wly$injected = new ArrayList<>();

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void wly$unlockAllTrades(ServerLevel level, CallbackInfo ci) {
        if (!WlyModule.on("visible-traders")) {
            return;
        }
        Villager self = (Villager) (Object) this;
        var offers = self.getOffers();

        // ① 先摘掉上一轮的假交易，避免升级时重复堆叠
        if (!wly$injected.isEmpty()) {
            offers.removeAll(wly$injected);
            wly$injected.clear();
        }

        // ② 重新补上「当前档以上」的全部交易，并标为缺货
        var data = self.getVillagerData();
        int cur = data.level();
        for (int lvl = cur + 1; lvl <= 5; lvl++) {
            var trades = data.profession().value().getTrades(lvl);
            if (trades == null) {
                continue;
            }
            int before = offers.size();
            ((AbstractVillagerInvoker) self).minecraftloveyou$invokeAddOffers(level, offers, trades);
            for (int i = before; i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                // 新增部分（低级村民看不到的高级交易）标记为缺货灰显；
                // 村民升级后 updateTrades 会重生成该档 → 正常可买
                while (!offer.isOutOfStock()) {
                    offer.increaseUses();
                }
                wly$injected.add(offer);
            }
        }
    }
}
