package com.minecraftloveyou.module.xppoints;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.player.Player;

/**
 * 经验值计费：把「消耗经验等级」的功能改为按**经验值（点）**结算。
 *
 * <p>覆盖范围：
 * <ul>
 *   <li><b>铁砧</b>（{@code AnvilMenu}）：
 *       <ul>
 *         <li>门槛 {@code mayPickup}：`experienceLevel &gt;= cost` → `经验值 &gt;= cost`；</li>
 *         <li>扣费 {@code onTake}：`giveExperienceLevels(-cost)` → `giveExperiencePoints(-cost)`。</li>
 *       </ul></li>
 *   <li><b>附魔台</b>（{@code EnchantmentMenu}）：
 *       <ul>
 *         <li>门槛 {@code clickMenuButton}：两处 `experienceLevel` 读取 → `经验值`；</li>
 *         <li>扣费：`onEnchantmentPerformed(stack, j)`（j = 1/2/3 级）
 *             → 保留原方法只为重置附魔种子（传 0 级），另按经验值扣 j 点。</li>
 *       </ul></li>
 * </ul>
 *
 * <p><b>为什么必须同时改门槛</b>：只改扣费的话，一个 40 级费用的铁砧操作仍然要求玩家有 40 级，
 * 玩家会认为「功能无效」。
 *
 * <p><b>经验值怎么算</b>：不能用 {@code Player.totalExperience} —— 它在 vanilla 里是「累计获得」
 * 的统计值，`giveExperienceLevels(-n)` **不会**把它减下去（javap 确认），拿它当货币会永远花不完。
 * 所以这里按经验条真实可花点数计算：`Σ 0..level-1 每级所需 + round(experienceProgress × 当前级所需)`，
 * 与 `Player.getXpNeededForNextLevel()` 的分段公式保持一致（7+2l / 37+5(l-15) / 112+9(l-30)）。
 */
public class XpPointsModule implements WlyModule {

    public static final String ID = "xp-points";

    public static void register() {
        ModuleManager.register(new XpPointsModule());
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

    /**
     * 玩家当前**可花**的经验值点数（经验条上的总点数）。
     * O(1)：三段闭式求和，等级再高也不会卡。
     */
    public static int of(Player player) {
        int level = player.experienceLevel;
        long total = 0L;
        total += segment(0, Math.min(level, 15), 7, 2, 0);
        total += segment(15, Math.min(level, 30), 37, 5, 15);
        total += segment(30, level, 112, 9, 30);
        total += Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /**
     * 求 level ∈ [from, to) 每级所需经验值之和，其中第 l 级需要 {@code base + step * (l - offset)}。
     * 例：0~14 级是 7 + 2l（base=7, step=2, offset=0）。
     */
    private static long segment(int from, int to, int base, int step, int offset) {
        if (to <= from) {
            return 0L;
        }
        long n = to - from;
        long first = base + (long) step * (from - offset);
        long last = base + (long) step * (to - 1 - offset);
        return n * (first + last) / 2L;
    }
}
