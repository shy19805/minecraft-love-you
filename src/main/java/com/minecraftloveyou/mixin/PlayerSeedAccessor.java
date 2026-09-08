package com.minecraftloveyou.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Player.enchantmentSeed 访问器（reroll 持久化种子）。 */
@Mixin(Player.class)
public interface PlayerSeedAccessor {

    @Accessor("enchantmentSeed")
    void minecraftloveyou$setEnchantmentSeed(int seed);
}
