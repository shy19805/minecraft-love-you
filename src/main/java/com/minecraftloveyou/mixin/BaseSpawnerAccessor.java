package com.minecraftloveyou.mixin;

import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** BaseSpawner.nextSpawnData 访问器（便携刷怪笼读取类型）。 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {

    @Accessor("nextSpawnData")
    SpawnData minecraftloveyou$getNextSpawnData();
}
