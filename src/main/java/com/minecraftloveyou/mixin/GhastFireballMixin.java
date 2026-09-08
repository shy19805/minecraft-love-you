package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 鎭堕瓊鐏�鐞冧笉鐮村潖鏂瑰潡锛氱垎鐐镐氦浜掔�?MOB 鏀逛负 NONE锛堜繚鐣欏疄浣撲激瀹筹級銆?*/
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
