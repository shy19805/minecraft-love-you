package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * 附魔大师 / 铁砧不会过于昂贵：解除铁砧 40 级 too-expensive
 * （createResult 内第 2 处 hasInfiniteMaterials 调用点）。
 *
 * <p>两个模块**共用这一组注入点**（{@link #wly$uncapped()} 做 OR 门控）：
 * 不重复注入，避免同一条指令被改两次；任一模块开启即生效。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /** 解除铁砧费用上限的模块门控：anvil-master（附魔大师）或 anvil-no-limit（铁砧不会过于昂贵）。 */
    private static boolean wly$uncapped() {
        return WlyModule.on("anvil-master") || WlyModule.on("anvil-no-limit");
    }

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z",
                    ordinal = 1))
    private boolean wly$noTooExpensive(boolean original) {
        return original || wly$uncapped();
    }

    /**
     * 待改造物品本身是堆叠时，vanilla 把费用钉死 40 —— 模块开时跳过该钳制。
     *
     * <p>⚠ ordinal 必须用 **1**。javap `createResult` 里 `ItemStack.getCount()` 共两处：
     * <ul>
     *   <li>ordinal 0（offset 232）：`while (i2 &gt; 0 &amp;&amp; j2 &lt; itemstack1.getCount())` ——
     *       **用材料修耐久的循环上界**，同时决定 `repairItemCountCost`（吃几个材料）。改它会变成
     *       「只吃 1 个材料、只修 1/4 耐久」；</li>
     *   <li>ordinal 1（offset 713）：`if (itemstack.getCount() &gt; 1) cost = 40;` —— 这才是要跳的钳制。</li>
     * </ul>
     */
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getCount()I",
                    ordinal = 1))
    private int wly$noPriceClamp(int original) {
        return wly$uncapped() ? 1 : original;
    }
}
