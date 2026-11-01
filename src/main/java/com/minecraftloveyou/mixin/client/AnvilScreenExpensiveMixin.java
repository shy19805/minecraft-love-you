package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 铁砧不会过于昂贵 · 客户端文案。
 *
 * <p>vanilla `AnvilScreen.extractLabels` 里：
 * <pre>
 *   int cost = menu.getCost();
 *   if (cost &gt; 0) {
 *       Component label = (cost &gt;= 40 &amp;&amp; !player.hasInfiniteMaterials())
 *               ? TOO_EXPENSIVE_TEXT                       // "过于昂贵！"
 *               : Component.translatable("container.repair.cost", cost);
 *   }
 * </pre>
 * 服务端那边已经放行了（{@code AnvilMenuMixin}），但界面仍会显示「过于昂贵！」——
 * 玩家会以为功能没生效。这里把这一处的 `hasInfiniteMaterials()` 判成 true，
 * 文案就永远走「费用 N」分支。
 *
 * <p>javap 核对：两版 `extractLabels` 里 `LocalPlayer.hasInfiniteMaterials()` 都只出现 **1 次**（offset 43）。
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenExpensiveMixin {

    @Redirect(method = "extractLabels", require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean wly$neverTooExpensive(LocalPlayer player) {
        if (WlyModule.on("anvil-no-limit") || WlyModule.on("anvil-master")) {
            return true;
        }
        return player.hasInfiniteMaterials();
    }
}
