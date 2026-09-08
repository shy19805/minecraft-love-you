package com.minecraftloveyou.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu 集成：模组列表点配置时打开 Cloth Config 界面。 */
public class WlyModMenuPlugin implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WlyConfig::createConfigScreen;
    }
}