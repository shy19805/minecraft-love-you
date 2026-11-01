package com.minecraftloveyou.client;

import com.minecraftloveyou.core.WlyModule;
import com.minecraftloveyou.module.powershot.PowershotModule;
import com.minecraftloveyou.module.trajectory.TrajectoryModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
 * 弹道预测的客户端实现（模块元数据在 {@code module/trajectory/TrajectoryModule}）。
 *
 * <p>支持：弓（按当前蓄力）、弩（已上弦）、三叉戟（蓄力 ≥10 刻）、
 * 雪球 / 鸡蛋 / 末影珍珠、喷溅与滞留药水、附魔之瓶。主手优先，主手不是投射物时看副手。
 *
 * <p><b>物理模型（与 {@code AbstractArrow} / {@code ThrowableProjectile} 的 tick 一致）</b>：
 * 每刻先 {@code pos += vel}，再 {@code vel *= 0.99}，最后 {@code vel.y -= gravity}。
 * 重力：箭 / 三叉戟 / 药水 0.05，雪球 / 鸡蛋 / 珍珠 0.03，附魔之瓶 0.07。
 *
 * <p><b>与力量射击联动</b>：powershot 会放大玩家投掷物的初速，所以预测初速也要乘同一个倍率，
 * 否则画出来的轨迹和实际飞行不一致。
 *
 * <p>纯客户端：只读世界 + 放粒子，不发数据包、不改游戏状态。
 */
public final class TrajectoryRenderer {

    /** 最多模拟多少刻（8 秒）；足够满蓄力弓飞完全程。 */
    private static final int MAX_STEPS = 160;
    /** 每几个模拟点放一颗粒子（越小越密、开销越大）。 */
    private static final int PARTICLE_STRIDE = 4;
    /** 路径颜色（浅蓝）。 */
    private static final int PATH_COLOR = 0x66CCFF;
    /** 落点颜色（红）。 */
    private static final int IMPACT_COLOR = 0xFF3B30;
    /**
     * 即使姿态没变，也最多每 N tick 重算一次路径。
     * 用来兜住「玩家站着不动、但世界被改了（放/挖方块）」的情况。
     */
    private static final int REFRESH_TICKS = 10;

    // ── 结果缓存 ──────────────────────────────────────────────────────────
    // 原来每 tick 无条件做 ≤160 次 level.clip + 全路径 AABB 取实体（最多 3200 次/秒），
    // 同时每 tick 新建 160 个 Vec3。改成：姿态/弹道参数没变就复用上一帧结果，
    // 只重放粒子（粒子每 tick 重发才看得见）。站着不动时开销≈0。
    private static int tickCounter = 0;
    private static long cachedSig = Long.MIN_VALUE;
    private static int cachedAtTick = -REFRESH_TICKS;
    private static Level cachedLevel = null;
    private static List<Vec3> cachedPath = List.of();
    private static int cachedEntityHit = -1;

    private TrajectoryRenderer() {
    }

    /** 客户端注册入口（WorldLovesYouClient 调用）。 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TrajectoryRenderer::onClientTick);
    }

    // ───────────────────────────── 主循环 ─────────────────────────────

    private static void onClientTick(Minecraft mc) {
        tickCounter++;
        if (!WlyModule.on(TrajectoryModule.ID) || mc.player == null || mc.level == null) {
            return;
        }
        if (ClientCompat.currentScreen() != null) {
            return; // 打开界面时不画（26.1.2 走 Minecraft.screen，26.2 走 Gui.screen()）
        }
        Player player = mc.player;
        Shot main = shotOf(player, InteractionHand.MAIN_HAND);
        if (main != null) {
            render(mc, player, main);
            return; // 主手已经在画了，不再画副手（避免重叠）
        }
        Shot off = shotOf(player, InteractionHand.OFF_HAND);
        if (off != null) {
            render(mc, player, off);
        }
    }

    // ───────────────────────────── 手持物 → 弹道参数 ─────────────────────────────

    /** 一次预测所需的弹道参数。 */
    private record Shot(double speed, double gravity) {
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
            return new Shot(power * 3.0D, 0.05D);
        }

        if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack) || player.getProjectile(stack).isEmpty()) {
                return null;
            }
            return new Shot(3.15D, 0.05D);
        }

        if (item instanceof TridentItem) {
            if (!player.isUsingItem() || !player.getUseItem().is(item)) {
                return null;
            }
            if (player.getTicksUsingItem() < 10) {
                return null; // 原版三叉戟不足 10 刻不会掷出
            }
            return new Shot(2.5D, 0.05D);
        }

        double speed = throwSpeed(item);
        if (speed <= 0.0D) {
            return null;
        }
        if (WlyModule.on("powershot")) {
            speed *= Math.max(0.0D, PowershotModule.powerVelocity);
        }
        return new Shot(speed, throwGravity(item));
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
        return 0.03D;
    }

    // ───────────────────────────── 模拟 + 渲染 ─────────────────────────────

    private static void render(Minecraft mc, Player player, Shot shot) {
        Level level = mc.level;
        if (level != cachedLevel) {           // 换世界/重进存档：缓存立刻失效
            cachedLevel = level;
            cachedSig = Long.MIN_VALUE;
            cachedPath = List.of();
            cachedEntityHit = -1;
        }

        long sig = signature(player, shot);
        boolean stale = sig != cachedSig || tickCounter - cachedAtTick >= REFRESH_TICKS;
        if (stale) {
            List<Vec3> path = simulate(level, player, shot);
            cachedPath = path;
            cachedEntityHit = path.isEmpty() ? -1 : firstEntityHit(level, player, path);
            cachedSig = sig;
            cachedAtTick = tickCounter;
        }

        List<Vec3> path = cachedPath;
        if (path.isEmpty()) {
            return;
        }

        // 命中实体则截断路径
        int entityHit = cachedEntityHit;
        int end = entityHit >= 0 ? entityHit + 1 : path.size();

        ParticleOptions dot = new DustParticleOptions(PATH_COLOR, 0.5F);
        for (int i = 0; i < end; i += PARTICLE_STRIDE) {
            Vec3 p = path.get(i);
            level.addParticle(dot, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
        }

        // 落点：实体命中优先，否则「路径提前结束」= 撞到了方块
        Vec3 impact = null;
        if (entityHit >= 0) {
            impact = path.get(entityHit);
        } else if (path.size() < MAX_STEPS) {
            impact = path.get(path.size() - 1);
        }
        if (impact != null) {
            ParticleOptions mark = new DustParticleOptions(IMPACT_COLOR, 0.9F);
            level.addParticle(mark, impact.x, impact.y, impact.z, 0.0D, 0.0D, 0.0D);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0D;
                level.addParticle(mark,
                        impact.x + Math.cos(a) * 0.3D, impact.y, impact.z + Math.sin(a) * 0.3D,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    /**
     * 弹道参数指纹：眼睛位置（0.05 精度）+ 视线方向（0.01 精度）+ 初速 + 重力。
     * 只要这四样没变，模拟结果就完全一样，可以直接复用上一帧。
     */
    private static long signature(Player player, Shot shot) {
        Vec3 e = player.getEyePosition();
        Vec3 v = player.getViewVector(1.0F);
        long h = 17L;
        h = h * 31L + Math.round(e.x * 20.0D);
        h = h * 31L + Math.round(e.y * 20.0D);
        h = h * 31L + Math.round(e.z * 20.0D);
        h = h * 31L + Math.round(v.x * 100.0D);
        h = h * 31L + Math.round(v.y * 100.0D);
        h = h * 31L + Math.round(v.z * 100.0D);
        h = h * 31L + Double.hashCode(shot.speed());
        h = h * 31L + Double.hashCode(shot.gravity());
        return h;
    }

    /** 按原版弹道物理推进，返回路径点（撞到方块时提前结束）。 */
    private static List<Vec3> simulate(Level level, Player player, Shot shot) {
        List<Vec3> path = new ArrayList<>();
        Vec3 pos = player.getEyePosition();
        Vec3 vel = player.getViewVector(1.0F).scale(shot.speed());
        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3 next = pos.add(vel);
            BlockHitResult hit = level.clip(new ClipContext(pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                return path; // 撞到方块：路径到此为止
            }
            path.add(next);
            vel = vel.scale(0.99D);
            vel = vel.add(0.0D, -shot.gravity(), 0.0D);
            pos = next;
        }
        return path;
    }

    /** 沿路径找最早的实体命中段序号；没有则返回 -1。 */
    private static int firstEntityHit(Level level, Player player, List<Vec3> path) {
        AABB box = boxOf(path).inflate(1.5D);
        List<Entity> candidates = level.getEntities(player, box,
                e -> e instanceof LivingEntity && e != player && e.isAlive());
        if (candidates.isEmpty()) {
            return -1;
        }
        int bestSegment = -1;
        for (Entity e : candidates) {
            AABB eb = e.getBoundingBox().inflate(0.3D);
            for (int i = 1; i < path.size(); i++) {
                if (bestSegment >= 0 && i >= bestSegment) {
                    break; // 已经找到更早的命中
                }
                if (eb.clip(path.get(i - 1), path.get(i)).isPresent()) {
                    bestSegment = i;
                    break;
                }
            }
        }
        return bestSegment;
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
