package com.minecraftloveyou.module.friendlyfire;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/** 友军保护：宠物免主人/同主宠物误伤；幼年动物免误伤（潜行绕过）。mixin/FriendlyFireMixin。 */
public class FriendlyFireModule implements WlyModule {

    public static final String ID = "friendly-fire";
    public static boolean protectPets = true;
    public static boolean protectBabies = true;
    public static boolean sneakBypass = true;

    public static void register() {
        ModuleManager.register(new FriendlyFireModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "wly.module." + ID;
    }
    

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin
    }
}