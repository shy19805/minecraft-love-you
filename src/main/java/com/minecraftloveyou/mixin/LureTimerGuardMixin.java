package com.minecraftloveyou.mixin;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 饵钓超上限后咬钩计时器死锁的兜底（配合 elb「附魔等级上限突破」）。
 *
 * <p><b>症状</b>：饵钓 ≥ 6 级的钓竿抛进水里<b>永远不咬钩</b>。
 *
 * <p><b>根因</b>（javap 26.1.2 / 26.2 同构，实测 lureSpeed=800、timeUntilLured=-359）：
 * {@code FishingRodItem.use} 里 {@code lureSpeed = (int)(getFishingTimeReduction() * 20f)}，
 * 而饵钓每级贡献 5.0 ⇒ <b>lureSpeed = 100 × 饵钓等级</b>。
 * {@code catchingFish} 的播种语句是 {@code timeUntilLured = Mth.nextInt(100,600) - lureSpeed}，
 * 饵钓 ≥ 6 时该值<b>恒 ≤ 0</b> ⇒ {@code timeUntilLured > 0} 分支永不进入 ⇒
 * {@code timeUntilHooked} 永不被赋值 ⇒ {@code nibble} 永远为 0 ⇒ 不咬钩。
 * 原版最高只有饵钓 3（lureSpeed=300，roll 落在 301~600 时仍为正），所以这个坑原版碰不到。
 *
 * <p><b>修法</b>（两步，缺一不可）：
 * ① {@code wly$saturateLureSpeed} 在构造入口把 lureSpeed 饱和到 5000，
 *    保证后面那个 {@code lureSpeed + 1} 永远不会溢出回绕；
 * ② {@code wly$lureNeverStarvesTheBiteTimer} 只把这一次掷点抬高到「减完 lureSpeed 后至少剩 1」，
 *    即返回 {@code max(roll, lureSpeed + 1)}，原版随后的 {@code - lureSpeed} 自然得到
 *    {@code max(roll - lureSpeed, 1)}。饵钓越高咬钩越快（符合附魔语义），且<b>任意等级</b>都不会死锁。
 * 不动 {@code nibble}（ordinal 0）与 {@code timeUntilHooked}（ordinal 1）两处掷点。
 * 全区间（lureSpeed 0 ~ Integer.MAX_VALUE）由自检 T17 逐档实测断言。
 */
@Mixin(FishingHook.class)
public abstract class LureTimerGuardMixin {

    /**
     * lureSpeed 饱和上限。掷点最大只有 600，所以 ≥ 600 之后 {@code max(roll − lureSpeed, 1)}
     * 已经恒等于 1（原版公式的地板），再高没有任何收益；
     * 而一旦放任到 {@code Integer.MAX_VALUE}，{@code lureSpeed + 1} 会溢出回绕成负数，
     * 且「被减数最大也只能是 MAX_VALUE、MAX_VALUE − MAX_VALUE = 0」使这条兜底在数学上无解。
     * 5000 ≈ 饵钓 50 级，留足余量。
     */
    private static final int WLY_LURE_SPEED_CAP = 5000;

    @Shadow
    private int lureSpeed;

    /**
     * 在构造入口就把 lureSpeed 饱和掉。
     *
     * <p>改的是<b>构造参数的局部槽</b>（index 4：this=0、EntityType=1、Level=2、luck=3、lureSpeed=4），
     * 所以真正写 final 域的那条 PUTFIELD 仍然是原版 {@code <init>} 自己的 ——
     * 若改成往 {@code <init>} 的 TAIL 注入一句赋值，Mixin 会把它放进独立的 handler 方法，
     * JVM 的 final 域检查按方法判定，运行期直接 {@code IllegalAccessError: Update to non-static
     * final field ... attempted from a different method than the initializer method <init>}（本机实测）。
     */
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;II)V",
            at = @At("HEAD"), argsOnly = true, index = 4, require = 1)
    private static int wly$saturateLureSpeed(int incoming) {
        if (!WlyModule.on("elb")) {
            return incoming;
        }
        return Math.min(incoming, WLY_LURE_SPEED_CAP);
    }

    /**
     * ordinal=2 的选取依据（catchingFish 内 Mth.nextInt 按出现顺序，两版偏移量一致）：
     * <table border="1">
     *   <tr><th>ordinal</th><th>字节码偏移</th><th>参数</th><th>语义</th></tr>
     *   <tr><td>0</td><td>538</td><td>(20, 40)</td><td>nibble：咬钩后的收杆窗口</td></tr>
     *   <tr><td>1</td><td>861</td><td>(20, 80)</td><td>timeUntilHooked：靠近到咬钩</td></tr>
     *   <tr><td>2</td><td>880</td><td>(100, 600)</td><td>timeUntilLured：被鱼发现 ← 本注入点</td></tr>
     * </table>
     */
    @Redirect(
            method = "catchingFish",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I",
                    ordinal = 2),
            require = 1)
    private int wly$lureNeverStarvesTheBiteTimer(RandomSource random, int min, int max) {
        int rolled = Mth.nextInt(random, min, max);
        if (!WlyModule.on("elb")) {
            return rolled;
        }
        return Math.max(rolled, this.lureSpeed + 1);
    }
}
