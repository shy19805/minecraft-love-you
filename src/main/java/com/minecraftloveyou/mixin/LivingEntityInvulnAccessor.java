package com.minecraftloveyou.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.2：invulnerableTime 字段在 Entity（父类）。 */
@Mixin(net.minecraft.world.entity.Entity.class)
public interface LivingEntityInvulnAccessor {

    @Accessor("invulnerableTime")
    void minecraftloveyou$setInvulnerableTime(int ticks);
}