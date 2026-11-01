package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 恶魂火球不破坏方块：爆炸交互由 MOB 改为 NONE（保留实体伤害）。 */
@Mixin(LargeFireball.class)
public abstract class GhastFireballMixin {

    @Redirect(
            method = "onHit",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/level/Level$ExplosionInteraction;MOB:Lnet/minecraft/world/level/Level$ExplosionInteraction;"))
    private Level.ExplosionInteraction wly$noGhastGrief() {
        if (WlyModule.on("eztweak-ghast-no-grief")) {
            return Level.ExplosionInteraction.NONE;
        }
        return Level.ExplosionInteraction.MOB;
    }
}
