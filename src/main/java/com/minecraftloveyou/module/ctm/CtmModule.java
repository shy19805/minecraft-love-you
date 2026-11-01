package com.minecraftloveyou.module.ctm;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 连接纹理/玻璃板修复（ctm）：玻璃、染色玻璃、玻璃板顶部去边框 → 相邻玻璃无缝（MVP）。
 * 说明：以**无边框玻璃贴图**实现（替换 assets/minecraft/textures/block/glass*.png），
 * 属设计文档认可的降级 MVP；强 CTM 阶段（Sodium 模型 API 烘焙）留待后续。
 */
public class CtmModule implements WlyModule {

    public static final String ID = "ctm";

    public static void register() {
        ModuleManager.register(new CtmModule());
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
    public boolean clientOnly() {
        return true; // 纯客户端贴图覆盖，专服无意义
    }

    /**
     * 配置界面不显示开关：贴图覆盖是静态资源，运行时开关不会改变任何东西
     * （见 {@link com.minecraftloveyou.core.WlyModule#hideToggle()}）。
     */
    @Override
    public boolean hideToggle() {
        return true;
    }

    @Override
    public void init(ModuleContext ctx) {
        // ⚠ 本模块的贴图覆盖（assets/minecraft/textures/block/glass*.png）是**静态资源**：
        //   只要 jar 在，覆盖就生效，开关无法在运行时撤销。
        //   真实控制点在「资源包」界面（F3+T 重载后生效）；此处仅作功能标记，
        //   文案已如实说明（wly.module.ctm.desc）。强 CTM（Sodium 模型烘焙）留待后续。
    }
}
