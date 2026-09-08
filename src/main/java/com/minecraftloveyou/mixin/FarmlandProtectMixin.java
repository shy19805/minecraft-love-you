package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 闃茶�曞湴韪╄笍锛歠allOn 鐩存帴杩斿洖锛堣�曞湴姘镐笉鍙樻偿鍦燂級銆?*/
@Mixin(FarmlandBlock.class)
public abstract class FarmlandProtectMixin {

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void wly$noTrample(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (WlyModule.on("eztweak-farmland-protect")) {
            ci.cancel();
        }
    }
}
