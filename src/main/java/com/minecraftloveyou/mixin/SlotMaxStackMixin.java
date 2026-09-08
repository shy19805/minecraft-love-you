package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.bigstacks.BigStacksModule;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 大堆叠兜底：Slot.getMaxStackSize(ItemStack) 提到配置值（min(container, item) 之后补一刀）。 */
@Mixin(Slot.class)
public abstract class SlotMaxStackMixin {

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void wly$slotMax(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (WlyModule.on("big-stacks") && stack != null && !stack.isEmpty()
                && !stack.isDamageableItem()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), BigStacksModule.maxStack));
        }
    }
}
