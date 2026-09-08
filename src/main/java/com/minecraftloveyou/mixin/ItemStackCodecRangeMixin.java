package com.minecraftloveyou.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 大堆叠：ItemStack.CODEC 的 count 范围 intRange(1,99)（<clinit> 静态初始化）→ 放宽到配置值。
 * 不改这里，服务端解码 set_creative_mode_slot 等包时 validatedStreamCodec 重编码校验 99 上限会踢线。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackCodecRangeMixin {

    @Redirect(method = "lambda$static$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;",
                    ordinal = 0),
            require = 1)
    private static Codec<Integer> wly$widenCountRange(int min, int max) {
        // <clinit> 阶段不能依赖模块开关（配置未加载会锁死 99）；运行时上限由 getMaxStackSize 钳制
        return Codec.intRange(1, 9_999_999);
    }
}
