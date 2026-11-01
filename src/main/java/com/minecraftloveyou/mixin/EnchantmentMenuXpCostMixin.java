package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.xppoints.XpPointsModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 经验值计费（xp-points）· 附魔台部分。
 *
 * <p>vanilla `clickMenuButton(player, slot)` 的判定（javap 逐条核对，两版一致）：
 * <pre>
 *   int j = slot + 1;                                     // 青金石数量 = 1/2/3
 *   if ((青金石不够) &amp;&amp; !player.hasInfiniteMaterials()) return false;   // offset 75
 *   if (costs[slot] &lt;= 0 || 输入为空
 *       || ((player.experienceLevel &lt; j                        // offset 100
 *            || player.experienceLevel &lt; costs[slot])           // offset 109
 *           &amp;&amp; !player.hasInfiniteMaterials())) return false;              // offset 122
 * </pre>
 *
 * <p>两处 `experienceLevel` 读取**都替换成经验值点数**：
 * 门槛变成 `经验值 &lt; j || 经验值 &lt; costs[slot]`。
 * 注意这两处的语义完全相同（都是「玩家等级」），替换值也相同 ——
 * 所以**即使将来版本把 ordinal 顺序调换，结果也不变**，不存在 #65 那种 ordinal 踩雷风险。
 *
 * <p>扣费不在 `clickMenuButton` 里，而在编译器生成的 lambda `lambda$clickMenuButton$0`：
 * `player.onEnchantmentPerformed(itemstack, j)`，j = 1/2/3 级。
 * 这个方法同时负责**重置附魔种子**（`enchantmentSeed = random.nextInt()`，javap 确认），
 * 所以不能整个跳过：改为传 0 级调用（保留种子重置），再单独按经验值扣 j 点。
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuXpCostMixin {

    @Redirect(method = "clickMenuButton", require = 1,
            at = @At(value = "FIELD", ordinal = 0,
                    target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I"))
    private int wly$gateWithXpPoints0(Player player) {
        return wly$gateWithXpPoints(player);
    }

    @Redirect(method = "clickMenuButton", require = 1,
            at = @At(value = "FIELD", ordinal = 1,
                    target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I"))
    private int wly$gateWithXpPoints1(Player player) {
        return wly$gateWithXpPoints(player);
    }

    private static int wly$gateWithXpPoints(Player player) {
        if (WlyModule.on(XpPointsModule.ID)) {
            return XpPointsModule.of(player);
        }
        return player.experienceLevel;
    }

    /**
     * 扣费：保留原方法（只为重置附魔种子，传 0 级），另按经验值扣 {@code levels} 点。
     *
     * <p><b>描述符必须用括号形式</b> {@code name(args)ret}：{@code MemberInfo.parse} 先找 {@code (}
     * 再找 {@code :}，写成 {@code onEnchantmentPerformed:(L...;I)V} 会把冒号粘进方法名
     * （解析成 {@code onEnchantmentPerformed:}）→ 运行期 {@code InvalidMemberDescriptorException}，
     * 且是**启动期硬崩**（不是静默失效）。冒号形式只对**字段**有效（如 {@code experienceLevel:I}）。
     */
    @Redirect(method = "lambda$clickMenuButton$0", require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V"))
    private void wly$payWithXpPoints(Player player, ItemStack stack, int levels) {
        if (WlyModule.on(XpPointsModule.ID)) {
            player.onEnchantmentPerformed(stack, 0);
            player.giveExperiencePoints(-levels);
            return;
        }
        player.onEnchantmentPerformed(stack, levels);
    }
}
