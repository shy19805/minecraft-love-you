package com.minecraftloveyou.client;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import com.minecraftloveyou.module.trajectory.TrajectoryModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 弹道预测（trajectory）的客户端实现；模块元数据在
 * {@code module/trajectory/TrajectoryModule}。
 *
 * <h2>为什么这样算才准</h2>
 * 预测必须逐刻复刻**原版 tick 的积分顺序**，而不是画一条解析抛物线：
 * <ul>
 *   <li><b>投掷物族</b>（雪球/鸡蛋/末影珍珠/药水/附魔之瓶，{@code ThrowableProjectile}）：
 *       {@code applyGravity()} → {@code applyInertia()} → 位移，即
 *       <code>v.y -= g; v *= 0.99; pos += v;</code>（26.2 字节码核对：tick() 先调 applyGravity 再调 applyInertia）；</li>
 *   <li><b>箭族</b>（箭/三叉戟，{@code AbstractArrow}）：先按当前速度位移，再
 *       {@code v *= 0.99}，最后 {@code v.y -= 0.05}。</li>
 * </ul>
 * 重力：雪球/鸡蛋/珍珠 0.03，药水 0.05，附魔之瓶 0.07，箭/三叉戟 0.05；空气阻力统一 0.99。
 *
 * <p>出射点也与原版一致：投掷物/箭都在
 * {@code (shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ())} 生成（不是眼睛位置，也不前移），
 * 方向用**原始 yaw/pitch**按 {@code shootFromRotation} 的公式重建（不用插值视线，避免半帧误差）。
 *
 * <p><b>与力量射击联动</b>：powershot 在 {@code Projectile#shootFromRotation} 处把初速乘以
 * {@link PowershotModule#powerVelocity}，且只作用于非箭类投掷物。预测侧用同一倍率、同一作用域，
 * 所以「开了力量射击」时画出的轨迹就是实际飞行轨迹（自检 T19 用真雪球反射调用断言该倍率）。
 */
public final class TrajectoryRenderer {

    /** 最多模拟多少刻（6 秒），够满蓄力弓/高倍率投掷物飞完。 */
    private static final int MAX_TICKS = 120;
    /** 超过这个距离就不再往后算（再远也看不清，且省开销）。 */
    private static final double MAX_DISTANCE = 72.0D;
    /** 相邻两颗粒子的间距（方块）。越小越连贯，但粒子数上升。 */
    private static final double PARTICLE_SPACING = 0.45D;
    /** 单帧最多放多少颗路径粒子（防止高倍率下刷屏）。 */
    private static final int MAX_PATH_PARTICLES = 140;
    /** 每隔几颗路径粒子补一颗发光粒子（END_ROD），让轨迹在暗处也看得见。 */
    private static final int GLOW_EVERY = 7;
    /** 落点标记的环半径。 */
    private static final double RING_RADIUS = 0.34D;

    private TrajectoryRenderer() {
    }

    /** 客户端注册入口（WorldLovesYouClient 调用）。 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TrajectoryRenderer::onClientTick);
    }

    // ───────────────────────────── 主循环 ─────────────────────────────

    private static void onClientTick(Minecraft mc) {
        if (!WlyModule.on(TrajectoryModule.ID) || mc.player == null || mc.level == null) {
            return;
        }
        if (ClientCompat.currentScreen() != null) {
            return; // 打开界面时不画（26.1.2 走 Minecraft.screen，26.2 走 Gui.screen()）
        }
        Player player = mc.player;
        Shot shot = shotOf(player, InteractionHand.MAIN_HAND);
        if (shot == null) {
            shot = shotOf(player, InteractionHand.OFF_HAND);
        }
        if (shot != null) {
            draw(mc, player, shot);
        }
    }

    // ───────────────────────── 手持物 → 弹道参数 ─────────────────────────

    /**
     * 一次预测所需的弹道参数。
     *
     * @param speed     初速（已含力量射击倍率）
     * @param gravity   每刻重力
     * @param drag      空气阻力（0.99）
     * @param moveFirst true=箭族（先位移再阻力再重力）；false=投掷物族（先重力再阻力再位移）
     * @param colorFrom 轨迹起点颜色（RGB）
     * @param colorTo   轨迹终点颜色（RGB）
     */
    private record Shot(double speed, double gravity, double drag, boolean moveFirst,
                        int colorFrom, int colorTo) {
    }

    private static Shot shotOf(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();

        if (item instanceof BowItem) {
            if (!player.isUsingItem() || !player.getUseItem().is(item)) {
                return null; // 没在拉弓
            }
            if (player.getProjectile(stack).isEmpty()) {
                return null; // 没有箭
            }
            int used = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            float power = BowItem.getPowerForTime(used);
            if (power < 0.1F) {
                return null;
            }
            // 原版：power * 3.0；箭族不受力量射击影响
            return new Shot(power * 3.0D, 0.05D, 0.99D, true, 0xCFE9FF, 0x4A9BFF);
        }

        if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack) || player.getProjectile(stack).isEmpty()) {
                return null;
            }
            return new Shot(3.15D, 0.05D, 0.99D, true, 0xD8F0FF, 0x3F8CFF);
        }

        if (item instanceof TridentItem) {
            if (!player.isUsingItem() || !player.getUseItem().is(item)) {
                return null;
            }
            if (player.getTicksUsingItem() < 10) {
                return null; // 原版三叉戟不足 10 刻不会掷出
            }
            return new Shot(2.5D, 0.05D, 0.99D, true, 0x9BFFE0, 0x1FA37A);
        }

        double base = throwSpeed(item);
        if (base <= 0.0D) {
            return null;
        }
        // 力量射击：与 PowershotMixin 完全同口径（只作用于非箭类投掷物）
        double speed = base;
        if (WlyModule.on(PowershotModule.ID)) {
            speed *= Math.max(0.0D, PowershotModule.powerVelocity);
        }
        if (speed <= 0.05D) {
            return null; // 倍率 0 = 扔脚底，没有可画的轨迹
        }
        int[] colors = throwColors(item);
        return new Shot(speed, throwGravity(item), 0.99D, false, colors[0], colors[1]);
    }

    private static double throwSpeed(Item item) {
        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            return 1.5D;
        }
        if (item instanceof ThrowablePotionItem) {
            return 0.5D; // 喷溅 / 滞留药水
        }
        if (item instanceof ExperienceBottleItem) {
            return 0.7D;
        }
        return 0.0D;
    }

    private static double throwGravity(Item item) {
        if (item instanceof ThrowablePotionItem) {
            return 0.05D;
        }
        if (item instanceof ExperienceBottleItem) {
            return 0.07D;
        }
        return 0.03D; // 雪球 / 鸡蛋 / 末影珍珠
    }

    private static int[] throwColors(Item item) {
        if (item instanceof EggItem) {
            return new int[]{0xFFF4C9, 0xD9A441};
        }
        if (item instanceof EnderpearlItem) {
            return new int[]{0xCFA6FF, 0x5B2E9E};
        }
        if (item instanceof ThrowablePotionItem) {
            return new int[]{0xFF9BDC, 0xA81E6E};
        }
        if (item instanceof ExperienceBottleItem) {
            return new int[]{0xC8FF7A, 0x3F9E33};
        }
        return new int[]{0xFFFFFF, 0x8FC9FF}; // 雪球
    }

    // ───────────────────────────── 模拟 + 渲染 ─────────────────────────────

    private static void draw(Minecraft mc, Player player, Shot shot) {
        Level level = mc.level;
        List<Vec3> path = simulate(level, player, shot);
        if (path.size() < 2) {
            return;
        }
        int end = entityHit(level, player, path);
        int last = end >= 0 ? end : path.size() - 1;

        spawnPath(level, path, last, shot);
        spawnImpact(level, path.get(last), shot);
    }

    /** 逐刻复刻原版积分，返回每刻位置；撞方块时最后一点是命中点。 */
    private static List<Vec3> simulate(Level level, Player player, Shot shot) {
        List<Vec3> path = new ArrayList<>();
        // 原版生成点：脚部 X/Z + 眼睛 Y 再下移 0.1
        Vec3 pos = new Vec3(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        Vec3 vel = direction(player).scale(shot.speed());
        path.add(pos);

        for (int i = 0; i < MAX_TICKS; i++) {
            if (!shot.moveFirst()) {
                // 投掷物族：先重力、再阻力、后位移
                vel = new Vec3(vel.x, vel.y - shot.gravity(), vel.z).scale(shot.drag());
            }
            Vec3 next = pos.add(vel);
            BlockHitResult hit = level.clip(new ClipContext(pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                return path;
            }
            path.add(next);
            pos = next;
            if (shot.moveFirst()) {
                // 箭族：先位移、再阻力、后重力
                Vec3 v = vel.scale(shot.drag());
                vel = new Vec3(v.x, v.y - shot.gravity(), v.z);
            }
            if (pos.distanceToSqr(path.get(0)) > MAX_DISTANCE * MAX_DISTANCE) {
                break;
            }
            if (pos.y < level.getMinY() - 8) {
                break;
            }
        }
        return path;
    }

    /**
     * 原版 {@code Projectile#shootFromRotation} 的方向公式（用原始 yaw/pitch，不用插值视线）：
     * <code>x = -sin(yaw)·cos(pitch)，y = -sin(pitch)，z = cos(yaw)·cos(pitch)</code>，再归一化。
     */
    private static Vec3 direction(Player player) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z).normalize();
    }

    /** 沿路径按固定间距放粒子（起点→终点颜色渐变），并每隔几颗补发光粒子。 */
    private static void spawnPath(Level level, List<Vec3> path, int last, Shot shot) {
        ParticleOptions dust = new DustColorTransitionOptions(shot.colorFrom(), shot.colorTo(), 0.62F);
        double travelled = 0.0D;
        double nextAt = 0.0D;
        int emitted = 0;
        Vec3 prev = path.get(0);
        for (int i = 1; i <= last && emitted < MAX_PATH_PARTICLES; i++) {
            Vec3 cur = path.get(i);
            double seg = prev.distanceTo(cur);
            while (seg > 1.0E-9D && nextAt <= travelled + seg && emitted < MAX_PATH_PARTICLES) {
                double t = (nextAt - travelled) / seg;
                Vec3 p = prev.lerp(cur, Math.max(0.0D, Math.min(1.0D, t)));
                level.addParticle(dust, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
                if (emitted % GLOW_EVERY == 0) {
                    level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
                }
                emitted++;
                nextAt += PARTICLE_SPACING;
            }
            travelled += seg;
            prev = cur;
        }
    }

    /** 落点标记：一圈尘 + 一撮暴击星 + 一根短竖线，远近都看得清。 */
    private static void spawnImpact(Level level, Vec3 p, Shot shot) {
        ParticleOptions ring = new DustColorTransitionOptions(shot.colorTo(), 0xFF3B30, 0.85F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0D;
            level.addParticle(ring,
                    p.x + Math.cos(a) * RING_RADIUS, p.y + 0.05D, p.z + Math.sin(a) * RING_RADIUS,
                    0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.CRIT, p.x, p.y + 0.1D, p.z,
                    (level.getRandom().nextDouble() - 0.5D) * 0.4D,
                    level.getRandom().nextDouble() * 0.3D,
                    (level.getRandom().nextDouble() - 0.5D) * 0.4D);
        }
        for (int i = 1; i <= 3; i++) {
            level.addParticle(ring, p.x, p.y + i * 0.22D, p.z, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * 沿路径找最早的实体命中段序号；没有则返回 -1。
     * 命中盒按原版投射物尺寸（约 0.25 半径）膨胀，避免「明明能打到却画穿过去」。
     */
    private static int entityHit(Level level, Player player, List<Vec3> path) {
        AABB box = boxOf(path).inflate(1.5D);
        List<Entity> candidates = level.getEntities(player, box,
                e -> e instanceof LivingEntity && e != player && e.isAlive());
        if (candidates.isEmpty()) {
            return -1;
        }
        int best = -1;
        for (Entity e : candidates) {
            AABB eb = e.getBoundingBox().inflate(0.3D);
            for (int i = 1; i < path.size(); i++) {
                if (best >= 0 && i >= best) {
                    break;
                }
                if (eb.clip(path.get(i - 1), path.get(i)).isPresent()) {
                    best = i;
                    break;
                }
            }
        }
        return best;
    }

    private static AABB boxOf(List<Vec3> path) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (Vec3 p : path) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
