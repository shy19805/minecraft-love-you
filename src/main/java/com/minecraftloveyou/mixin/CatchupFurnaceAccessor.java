package com.minecraftloveyou.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 离线追赶：读取熔炉的「剩余燃烧刻数」，用于补偿循环的提前终止
 * （没有燃料在烧时不必继续空转）。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface CatchupFurnaceAccessor {

    @Accessor("litTimeRemaining")
    int wly$litTimeRemaining();
}