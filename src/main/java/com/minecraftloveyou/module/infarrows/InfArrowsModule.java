package com.minecraftloveyou.module.infarrows;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 所有箭配无限：带“无限”附魔的弓/弩使用任意箭种（药箭/光灵箭等）也不再消耗弹药。
 * 实际逻辑在 mixin/InfinityArrowsMixin（26.2 消耗统一走 EnchantmentHelper.processAmmoUse）。
 *
 * 联动子项 freeArrowWithoutStock（见 mixin/InfinityFreeArrowMixin）：
 * 背包里一支箭都没有时也允许上弦发射。开关值由配置层（WlyConfigData）恢复。
 */
public class InfArrowsModule implements WlyModule {

    public static final String ID = "infinity-arrows";

    /** 子项：无限不需要背包有箭。默认开（配置未写入该项时生效）。 */
    public static boolean freeArrowWithoutStock = true;

    public static void register() {
        ModuleManager.register(new InfArrowsModule());
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
        // 行为在 mixin 中
    }
}