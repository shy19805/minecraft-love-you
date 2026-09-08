package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/** 附魔大师：解除铁砧 40 级 too-expensive（createResult 内第 2 处 hasInfiniteMaterials 调用点）。 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z",
                    ordinal = 1))
    private boolean wly$noTooExpensive(boolean original) {
        return original || WlyModule.on("anvil-master");
    }

    /** 多本附魔书叠加时 vanilla 把价格钉死 40 —— 模块开时跳过该钳制。 */
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getCount()I",
                    ordinal = 0))
    private int wly$noPriceClamp(int original) {
        return WlyModule.on("anvil-master") ? 1 : original;
    }
}
