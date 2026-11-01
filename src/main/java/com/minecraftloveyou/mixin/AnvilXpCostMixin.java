package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.xppoints.XpPointsModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 经验值计费（xp-points）· 铁砧部分。
 *
 * <p>铁砧有两处「按等级」的判定，**必须同时改**，否则只改一处会让玩家觉得功能无效：
 * <ol>
 *   <li><b>门槛</b> {@code mayPickup}：`(hasInfiniteMaterials || player.experienceLevel &gt;= cost) &amp;&amp; cost &gt; 0`。
 *       javap 确认本方法内 `Player.experienceLevel` 只被读 **1 次**（offset 8），
 *       直接 Redirect 这个 GETFIELD → 换成「经验值点数」。
 *       改完表达式变成 `(hasInfiniteMaterials || 经验值 &gt;= cost) &amp;&amp; cost &gt; 0`。</li>
 *   <li><b>扣费</b> {@code onTake}：`player.giveExperienceLevels(-this.cost.get())`。
 *       Redirect 成 `giveExperiencePoints(-cost)` —— 按经验值扣，
 *       由 `Player.giveExperiencePoints` 自己处理「进度扣成负数就掉级」。</li>
 * </ol>
 *
 * <p>两版的 `mayPickup` / `onTake` 字节码已用 javap 逐条比对，完全一致。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilXpCostMixin {

    /** 门槛：把「等级」换成「经验值点数」。 */
    @Redirect(method = "mayPickup", require = 1,
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I"))
    private int wly$gateWithXpPoints(Player player) {
        if (WlyModule.on(XpPointsModule.ID)) {
            return XpPointsModule.of(player);
        }
        return player.experienceLevel;
    }

    /**
     * 扣费：把「扣等级」换成「扣经验值」。{@code levels} 是负数。
     *
     * <p><b>描述符必须用括号形式</b> {@code name(args)ret}：{@code MemberInfo.parse} 先找 {@code (}
     * 再找 {@code :}，写成 {@code giveExperienceLevels:(I)V} 会把冒号粘进方法名
     * （解析成 {@code giveExperienceLevels:}）→ 运行期 {@code InvalidMemberDescriptorException}。
     * 冒号形式只对**字段**有效（如 {@code experienceLevel:I}）。
     */
    @Redirect(method = "onTake", require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    private void wly$payWithXpPoints(Player player, int levels) {
        if (WlyModule.on(XpPointsModule.ID)) {
            player.giveExperiencePoints(levels);
            return;
        }
        player.giveExperienceLevels(levels);
    }
}
