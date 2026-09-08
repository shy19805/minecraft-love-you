package com.minecraftloveyou.mixin.client;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** MerchantScreen.scrollOff 访问器（trade-display 用）。 */
@Mixin(MerchantScreen.class)
public interface MerchantScreenScrollAccessor {

    @Accessor("scrollOff")
    int minecraftloveyou$getScrollOff();
}
