package com.minecraftloveyou.mixin;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * CropBlock.getBaseSeedId Invoker（连锁收获「补种」用）。
 *
 * <p>为什么需要它：`Blocks.WHEAT.asItem()` 是**小麦**而不是**小麦种子**，
 * `Blocks.BEETROOTS.asItem()` 同理。早期实现直接用 `block.asItem()` 当种子去背包里找，
 * 结果小麦/甜菜永远「找不到种子」→ 只收不补种。原版真正记录种子的是
 * `CropBlock.getBaseSeedId()`（protected），只能靠 Invoker 取。
 */
@Mixin(CropBlock.class)
public interface CropBlockInvoker {

    @Invoker("getBaseSeedId")
    ItemLike wly$getBaseSeedId();
}
