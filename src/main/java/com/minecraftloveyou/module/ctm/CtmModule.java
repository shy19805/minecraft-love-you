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
    public void init(ModuleContext ctx) {
        // 通过静态资源覆盖（assets/minecraft/textures/block/glass*.png 去边框）实现无缝玻璃。
        // 注意：纹理为静态资源，无法在运行时切换；关掉本模块后重启才会恢复原版边框（MVP 限制，强 CTM 留后续）。
    }
}
