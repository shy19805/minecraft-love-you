package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.bigstacks.BigStacksModule;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 接口 default 方法体注入（require=1 强校验）。 */
@Mixin(ItemInstance.class)
public interface ItemInstanceMaxStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, require = 1)
    private void wly$ifaceBigStack(CallbackInfoReturnable<Integer> cir) {
        if (!WlyModule.on("big-stacks")) {
            return;
        }
        Object self = this;
        if (self instanceof net.minecraft.core.component.DataComponentHolder holder
                && holder.has(net.minecraft.core.component.DataComponents.MAX_DAMAGE)) {
            return; // 耐久物品（工具/武器/盔甲）不可堆；其余（含附魔书）可堆
        }
        cir.setReturnValue(Math.max(cir.getReturnValueI(), com.minecraftloveyou.module.bigstacks.BigStacksModule.maxStack));
    }
}
