package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.RomanNumeral;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 附魔大师：等级显示罗马数字（覆盖 vanilla 的 level.N 翻译，>10 也显示罗马字）。 */
@Mixin(Enchantment.class)
public abstract class EnchantmentFullNameMixin {

    @Inject(method = "getFullname", at = @At("RETURN"), cancellable = true)
    private static void wly$romanLevel(net.minecraft.core.Holder<Enchantment> enchantment, int level, CallbackInfoReturnable<Component> cir) {
        if (!WlyModule.on("anvil-master") || level <= 1) {
            return;
        }
        Component old = cir.getReturnValue();
        var siblings = old.getSiblings();
        if (siblings.isEmpty()) {
            return;
        }
        // 保留 root（附魔名）样式与内容；只把末尾 level 组件换成罗马数字
        var rebuilt = old.copy();
        var list = rebuilt.getSiblings();
        Component last = list.get(list.size() - 1);
        list.set(list.size() - 1, Component.literal(RomanNumeral.of(level)).withStyle(last.getStyle()));
        cir.setReturnValue(rebuilt);
    }
}
