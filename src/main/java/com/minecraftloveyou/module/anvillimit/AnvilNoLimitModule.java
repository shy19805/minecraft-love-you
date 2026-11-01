package com.minecraftloveyou.module.anvillimit;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;

/**
 * 铁砧不会过于昂贵：解除铁砧的两道「贵到不能用」的墙。
 *
 * <ul>
 *   <li><b>40 级硬上限</b>：vanilla `createResult` 里 `cost &gt;= 40 &amp;&amp; !hasInfiniteMaterials`
 *       → 结果槽置空（界面显示「过于昂贵！」），物品根本取不出来；</li>
 *   <li><b>堆叠物品钉死 40</b>：待改造物品本身是堆叠时，vanilla 直接把费用钳成 40。</li>
 * </ul>
 *
 * <p>行为在 {@code mixin/AnvilMenuMixin}（与 {@code anvil-master} **共用同一组注入点**，
 * 两者任一开启即生效——不重复注入，避免同一条指令被改两次）。
 * 界面上的「过于昂贵！」文案另由 {@code mixin/client/AnvilScreenExpensiveMixin} 一并去掉。
 *
 * <p>注意：本模块只解除「不能取」的限制，**费用照付**。
 * 想让费用按经验值（而不是等级）结算，再开 {@code xp-points}。
 */
public class AnvilNoLimitModule implements WlyModule {

    public static final String ID = "anvil-no-limit";

    public static void register() {
        ModuleManager.register(new AnvilNoLimitModule());
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
        // 行为全部在 mixin，无需事件监听
    }
}
