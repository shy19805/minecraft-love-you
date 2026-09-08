package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.bigstacks.BigStacksModule;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 大堆叠：容器层 26.2 默认 99 → 配置上限（Slot= min(container, item)）。 */
@Mixin(Container.class)
public interface ContainerMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void wly$containerMaxStack(CallbackInfoReturnable<Integer> cir) {
        if (WlyModule.on("big-stacks")) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), BigStacksModule.maxStack));
        }
    }
}
