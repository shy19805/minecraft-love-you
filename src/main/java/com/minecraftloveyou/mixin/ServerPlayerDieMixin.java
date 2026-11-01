package com.minecraftloveyou.mixin;

import com.minecraftloveyou.module.graves.GraveHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 墓碑：在 ServerPlayer#die 最开头摘走全部背包+装备+经验到暂存（GraveHolder），
 * 原版掉落（dropAll）见到的背包已是空 → 物品不再散落；随后 GravesModule 用暂存生成墓碑。
 *
 * <p><b>keepInventory 必须放行</b>：原版在 {@code keepInventory=true} 时根本不掉落物品。
 * 早期版本无条件摘背包，结果是「开了保留物品，死亡后背包照样被清空、还得跑回去捡墓碑」——
 * 功能与游戏规则打架。这里先判规则，保留物品时直接不介入。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDieMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void wly$captureInventoryBeforeDrop(DamageSource source, CallbackInfo ci) {
        if (!com.minecraftloveyou.core.WlyModule.on("graves")) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return; // 原版不会掉落：此时生成墓碑等于把玩家背包搬走，必须放行
        }
        GraveHolder.capture(self);
    }
}
