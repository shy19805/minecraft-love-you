package com.minecraftloveyou.module.offlinecatchup;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 离线追赶（offline-catchup）：参考 Unloaded Activity —— 区块卸载 / 服务器停机期间，
 * 熔炉继续烧炼、营火继续烤、方块继续走随机刻、幼年动物继续长大；重新加载时按
 * 「现实时间」一次性补上。
 *
 * <p>时间基准（两个来源，取较新者）：
 * <ul>
 *   <li><b>心跳时间戳</b>：服务器运行期间每 {@link #HEARTBEAT_MS} 毫秒把当前毫秒写入世界目录的
 *       {@code wly_offline_catchup.txt}，停机时再写一次精确值。
 *       <b>必须用心跳，不能只在停机时写</b>：玩家强杀进程 / 游戏崩溃（本项目最常发生的情况——
 *       「卡在加载地形中」被强杀）时 {@code SERVER_STOPPING} 不会触发，
 *       只靠停机写入会导致下一次进世界「零补偿」，表现为功能完全无效。</li>
 *   <li><b>区块卸载时刻</b>：区块 unload 时在内存里记下时间戳（{@code LAST_SEEN}），
 *       加载时取出 → 覆盖「玩家跑远、区块卸载后再回来」的场景。</li>
 * </ul>
 *
 * <p><b>执行模型（v2，修「卡在加载地形中」）</b>：
 * {@code CHUNK_LOAD} / {@code ENTITY_LOAD} 回调里**只入队，不碰世界**；
 * 真正的补偿在 {@link ServerTickEvents#END_SERVER_TICK} 里用 {@code getChunkNow} 取回区块后执行，
 * 每 tick 有时间预算。
 *
 * <p>为什么必须这样：熔炉 {@code serverTick} 内部会 {@code ServerLevel.setBlock(pos, state, 3)}
 * （更新邻居），而 {@code CHUNK_LOAD} 是在区块仍被 {@code ChunkMap} 装载时触发的；
 * 此刻写方块会去取相邻区块 → {@code ServerChunkCache.getChunk(...).join()} 重入装载流程 →
 * **死锁**。症状就是「卡在加载地形中：37%」不动（37% 正是 {@code Loading 49 chunks for player spawn} 那一步）。
 *
 * <p><b>四条补偿触发通路（v1.2.3 起）</b>——「时间流逝但世界没推进」有三种完全不同的成因，
 * 只覆盖第一种会让玩家觉得「功能时灵时不灵」：
 * <ol>
 *   <li><b>区块卸载 / 服务器停机</b>（{@code CHUNK_LOAD}）：玩家跑远、退出重进、重启游戏。
 *       时间基准取「停机心跳戳」与「区块卸载时刻」的较新者。</li>
 *   <li><b>实体重新加载</b>（{@code ENTITY_LOAD}）：动物随区块回来时补年龄。</li>
 *   <li><b>世界停摆</b>（{@link #compensateWorldStall}）：游戏**一直开着、区块从未卸载**，
 *       但现实时间在流逝——暂停菜单 / 窗口失焦自动暂停 / 系统睡眠休眠 / 长时间卡死。
 *       判定方式：服务器两次 tick 之间的现实时间差 ≥ {@link #STALL_MIN_MS}。
 *       这类情况**不会**触发 {@code CHUNK_LOAD}，旧实现下完全零补偿。</li>
 *   <li><b>睡觉跳过夜晚</b>（{@link #compensateSleepSkip}）：玩家用床跳过夜晚时，
 *       被跳过的那段世界时钟（{@link #SLEEP_SKIP_MIN_TICKS}~{@link #SLEEP_SKIP_MAX_TICKS} 刻）
 *       在原版里是**直接抹掉**的，作物/熔炉都不推进。这里按「时间照常流逝」补给玩家周围的区块。
 *       判定方式：世界时钟（{@code Level.getOverworldClockTime()}）单 tick 前跳。</li>
 * </ol>
 */
public class OfflineCatchupModule implements WlyModule {

    public static final String ID = "offline-catchup";

    /** 少于 1 分钟不补偿（避免频繁加载/卸载时的无意义开销）。 */
    public static final long MIN_DELTA_MS = 60_000L;
    /** 最多按 7 天补偿（防时间戳异常导致巨量推进）。 */
    public static final long MAX_DELTA_MS = 7L * 24L * 3600_000L;
    /** 熔炉单区块单次补偿的 tick 上限（约 2.8 小时），其余靠提前终止收敛。 */
    public static final long MAX_FURNACE_TICKS = 200_000L;
    /** 动物单次补偿的年龄推进上限（秒）。年龄以 tick 计、20 tick = 1 秒。 */
    public static final int MAX_AGE_SECONDS = 7200;

    /** 心跳写盘间隔（毫秒）：即使被强杀，最多也只丢失这段时间的离线补偿。 */
    private static final long HEARTBEAT_MS = 15_000L;

    /** 每 tick 用于补偿的时间预算（纳秒）：8ms ≈ 半个 tick，避免抢走游戏主逻辑的时间。 */
    private static final long TICK_BUDGET_NS = 8_000_000L;
    /** 每 tick 最多处理的区块数。 */
    private static final int MAX_CHUNKS_PER_TICK = 12;
    /** 每 tick 最多处理的实体数。 */
    private static final int MAX_ENTITIES_PER_TICK = 64;
    /** 队列上限：极端批量加载（如 Voxy 一次灌入数万区块）时丢弃多余请求，保证不拖垮服务器。 */
    private static final int MAX_QUEUE = 8192;
    /** 服务器启动后多久（tick）关闭「停机时间戳」窗口，之后只按区块自身时间戳计算。 */
    private static final int STAMP_GRACE_TICKS = 600;
    /** 窗口的硬上限：无论队列是否清空，超过这么久也关闭。 */
    private static final int STAMP_HARD_LIMIT_TICKS = 1800;
    /** 统计日志最长静默多少 tick 后强制输出一次（防止长时间不刷屏）。 */
    private static final int STATS_FLUSH_TICKS = 200;

    /**
     * 服务器两次 tick 之间的现实时间超过此值 → 判定为「世界停摆」。
     * 正常 tick 是 50ms，10 秒意味着世界被冻结（暂停 / 失焦 / 系统休眠 / 长卡顿）。
     */
    private static final long STALL_MIN_MS = 10_000L;
    /** 单次停摆最多补偿的现实时长（12 小时），防止系统时钟跳变导致巨量推进。 */
    private static final long STALL_MAX_MS = 12L * 3600_000L;
    /** 睡觉跳过夜晚：世界时钟单 tick 前跳超过这么多刻即认定为「跳过夜晚」（正常每 tick +1）。 */
    private static final long SLEEP_SKIP_MIN_TICKS = 100L;
    /** 睡觉补偿的刻数上限：24000 = 一整天。超过说明是 {@code /time set} 之类的指令跳变，不补偿。 */
    private static final long SLEEP_SKIP_MAX_TICKS = 24_000L;
    /** 停摆 / 睡觉补偿的玩家半径（区块）。10 区块 = 160 格，覆盖玩家自家农田与机器区。 */
    private static final int BURST_RADIUS_CHUNKS = 10;
    /**
     * 两次「睡觉补偿」之间的最小间隔（tick）。
     * 防的是**时钟倍速类模组**：若某个模组把世界时钟速率调到 100×，则每 tick 都前跳 100 刻，
     * 会被误判成「一直在跳过夜晚」并每 tick 灌满队列。加冷却后最坏情况每 10 秒补 100 刻
     * （折算随机刻期望 ≈ 0.07 次/方块 → 实际零生长），完全无害。
     */
    private static final int SLEEP_COOLDOWN_TICKS = 200;

    private static final String STAMP_FILE = "wly_offline_catchup.txt";
    /**
     * 区块 → 最后活跃时刻。**必须有界**：区块卸载/加载在长会话里会触发几十万次，
     * 早期用裸 ConcurrentHashMap 只增不减（跨世界、跨会话都留在静态字段里）。
     * 改成 LRU（访问序 LinkedHashMap + 超限淘汰最旧），上限 16384 条 ≈ 1MB 以内。
     */
    private static final int MAX_LAST_SEEN = 16384;
    private static final Map<String, Long> LAST_SEEN = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(256, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_LAST_SEEN;
                }
            });
    private static volatile long stopStamp = 0L;
    private static volatile long clearStampAtTick = Long.MAX_VALUE;
    private static volatile long lastHeartbeatMs = 0L;
    private static volatile boolean heartbeatWarned = false;
    /** 上一次服务器 tick 的现实时刻（毫秒）。用于检测「世界停摆」。 */
    private static volatile long lastTickWallMs = 0L;
    /** 上一次看到的世界时钟（刻）。-1 = 尚未建立基准。用于检测「睡觉跳过夜晚」。 */
    private static volatile long lastOverworldClock = -1L;
    /** 下一次允许做「睡觉补偿」的 tick。见 {@link #SLEEP_COOLDOWN_TICKS}。 */
    private static volatile int nextSleepAllowedTick = 0;

    private static final ConcurrentLinkedQueue<PendingChunk> CHUNK_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<PendingEntity> ENTITY_QUEUE = new ConcurrentLinkedQueue<>();

    /** 待补偿区块（只存坐标，不持有区块引用）。 */
    private record PendingChunk(ServerLevel level, int x, int z, long deltaMs) {
    }

    /** 待补偿实体（弱语义：处理时重新校验存活状态）。 */
    private record PendingEntity(ServerLevel level, AgeableMob mob, long deltaMs) {
    }

    // 统计（跨 tick 累加，批量输出一行）
    private static long statChunks = 0L;
    private static long statAnimals = 0L;
    private static long statFurnaces = 0L;
    private static long statCampfires = 0L;
    private static long statGrowth = 0L;
    private static long statMisc = 0L;
    private static long statMaxDeltaMs = 0L;
    private static int statLastFlushTick = 0;

    public static void register() {
        ModuleManager.register(new OfflineCatchupModule());
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
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    // ───────────────────────────── 时间戳 ─────────────────────────────

    private static String key(ServerLevel level, int x, int z) {
        return level.dimension().identifier() + ":" + x + "," + z;
    }

    private static Path stampPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(STAMP_FILE);
    }

    private static void writeStamp(MinecraftServer server, long value) {
        try {
            Files.writeString(stampPath(server), Long.toString(value), StandardCharsets.UTF_8);
            lastHeartbeatMs = value;
        } catch (Exception e) {
            if (!heartbeatWarned) {
                heartbeatWarned = true;
                WorldLovesYouMod.LOGGER.warn("[世界爱你] 离线追赶：写入心跳时间戳失败：{}", e.toString());
            }
        }
    }

    private void onServerStarting(MinecraftServer server) {
        stopStamp = 0L;
        clearStampAtTick = Long.MAX_VALUE;
        lastHeartbeatMs = 0L;
        heartbeatWarned = false;
        lastTickWallMs = 0L;
        lastOverworldClock = -1L;
        nextSleepAllowedTick = 0;
        LAST_SEEN.clear();
        CHUNK_QUEUE.clear();
        ENTITY_QUEUE.clear();
        // 模块关闭：不读停机戳 → stopStamp 保持 0 → 后续一个区块都不补偿
        if (!on()) {
            return;
        }
        try {
            Path p = stampPath(server);
            if (Files.exists(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                long v = Long.parseLong(s);
                long now = System.currentTimeMillis();
                // 时钟回拨 / 时间戳异常保护
                stopStamp = (v > 0L && v <= now + 60_000L) ? v : 0L;
            }
        } catch (Exception e) {
            WorldLovesYouMod.LOGGER.warn("[世界爱你] 离线追赶：读取停机时间戳失败：{}", e.toString());
        }
    }

    private void onServerStarted(MinecraftServer server) {
        if (!on()) {
            return;
        }
        // 出生区块已在 prepareLevels 阶段入队；给它们一个处理窗口，之后停用停机时间戳，
        // 否则玩家本次游玩中新加载的远处区块会全部按「停机至今」重复补偿。
        clearStampAtTick = server.getTickCount() + STAMP_GRACE_TICKS;
        statLastFlushTick = server.getTickCount();
        if (stopStamp > 0L) {
            WorldLovesYouMod.LOGGER.info("[世界爱你] 离线追赶：检测到上次退出时间，离线 {} 分钟，开始补偿",
                    Math.max(0L, (System.currentTimeMillis() - stopStamp) / 60_000L));
        }
        writeStamp(server, System.currentTimeMillis());
    }

    /**
     * 停机时间戳**故意不加模块门控**：
     * 15 秒心跳（{@code onEndServerTick}）已经门控，这里只在停服时写一次文件。
     * 保持时间基准新鲜，用户中途打开本模块时不会读到几天前的旧戳、一次性补偿到上限。
     */
    private void onServerStopping(MinecraftServer server) {
        writeStamp(server, System.currentTimeMillis());
    }

    /** 该区块可确认的最后活跃时刻；0 表示「没有任何时间基准」。 */
    public static long lastActive(ServerLevel level, int x, int z) {
        Long mem = LAST_SEEN.get(key(level, x, z));
        return Math.max(mem == null ? 0L : mem, stopStamp);
    }

    /** 更新该区块的最后活跃时刻。 */
    public static void touch(ServerLevel level, int x, int z, long now) {
        LAST_SEEN.put(key(level, x, z), now);
    }

    /** 该区块相对「上次活跃」已离线多少毫秒；≤0 表示不需要补偿。 */
    private static long deltaOf(ServerLevel level, int x, int z, long now) {
        long base = lastActive(level, x, z);
        if (base <= 0L) {
            return 0L; // 没有基准（本会话首次遇到且没有心跳时间戳）→ 不补偿
        }
        return Math.min(now - base, MAX_DELTA_MS);
    }

    // ───────────────────────────── 事件：只入队 ─────────────────────────────

    private void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean newChunk) {
        if (!on() || newChunk) {
            return; // 新生成区块没有「离线历史」，无需补偿
        }
        long now = System.currentTimeMillis();
        int cx = chunk.getPos().x();
        int cz = chunk.getPos().z();
        long delta = deltaOf(level, cx, cz, now);
        // 注意：这里**不** touch。区块自身的补偿要等 END_SERVER_TICK 才执行，
        // 而同区块的实体是在 CHUNK_LOAD 之后立刻触发的 ENTITY_LOAD —— 若此处先 touch，
        // 实体侧算出来的 delta 恒为 0，动物年龄补偿会永远失效。
        if (delta < MIN_DELTA_MS) {
            return;
        }
        if (CHUNK_QUEUE.size() >= MAX_QUEUE) {
            return;
        }
        CHUNK_QUEUE.add(new PendingChunk(level, cx, cz, delta));
    }

    private void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        if (!on()) {
            return;
        }
        touch(level, chunk.getPos().x(), chunk.getPos().z(), System.currentTimeMillis());
    }

    private void onEntityLoad(Entity entity, ServerLevel level) {
        if (!on() || !(entity instanceof AgeableMob mob)) {
            return;
        }
        long now = System.currentTimeMillis();
        int cx = entity.chunkPosition().x();
        int cz = entity.chunkPosition().z();
        long delta = deltaOf(level, cx, cz, now);
        if (delta < MIN_DELTA_MS) {
            return;
        }
        if (ENTITY_QUEUE.size() >= MAX_QUEUE) {
            return;
        }
        ENTITY_QUEUE.add(new PendingEntity(level, mob, delta));
    }

    // ───────────────────────────── 事件：真正干活 ─────────────────────────────

    private void onEndServerTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        long gap = lastTickWallMs == 0L ? 0L : now - lastTickWallMs;
        lastTickWallMs = now;
        long clock = worldClock(server);
        long clockJump = (clock < 0L || lastOverworldClock < 0L) ? 0L : clock - lastOverworldClock;
        if (clock >= 0L) {
            lastOverworldClock = clock;
        }
        // 模块关闭时也要维护上面两个时间基准，否则中途打开模块会把「已经流逝的现实时间」
        // 当成一次停摆 / 一次睡觉跳过，一次性巨量补偿。
        if (!on()) {
            return;
        }
        heartbeat(server);
        if (gap >= STALL_MIN_MS) {
            compensateWorldStall(server, Math.min(gap, STALL_MAX_MS));
        }
        if (clockJump >= SLEEP_SKIP_MIN_TICKS && clockJump <= SLEEP_SKIP_MAX_TICKS) {
            int tick = server.getTickCount();
            if (tick >= nextSleepAllowedTick) {
                nextSleepAllowedTick = tick + SLEEP_COOLDOWN_TICKS;
                compensateSleepSkip(server, clockJump * 50L);
            }
        }
        long deadline = System.nanoTime() + TICK_BUDGET_NS;
        drainChunks(deadline);
        drainEntities(deadline);
        expireStamp(server);
        flushStats(server, false);
    }

    /** 世界时钟（刻）。取不到时返回 -1（当作「本次没有基准」，不参与跳变判定）。 */
    public static long worldClock(MinecraftServer server) {
        try {
            return server.overworld().getOverworldClockTime();
        } catch (Throwable t) {
            return -1L;
        }
    }

    /**
     * 「世界停摆」补偿：游戏一直开着、区块从未卸载，但现实时间在流逝。
     * 覆盖暂停菜单、窗口失焦自动暂停、系统睡眠/休眠、长时间卡死。
     * 这类情况不会触发 {@code CHUNK_LOAD}，只能靠 tick 之间的现实时间差发现。
     */
    private void compensateWorldStall(MinecraftServer server, long gapMs) {
        int chunks = enqueueAroundPlayers(server, gapMs);
        if (chunks > 0) {
            // 立刻刷新心跳：万一玩家紧接着强杀进程，下次进世界不会把这段停摆重复补偿一遍。
            writeStamp(server, System.currentTimeMillis());
            WorldLovesYouMod.LOGGER.info(
                    "[世界爱你] 离线追赶：检测到世界停摆 {}（暂停 / 窗口失焦 / 系统休眠 / 长时间卡顿），补偿玩家周围 {} 个区块",
                    human(gapMs), chunks);
        }
    }

    /**
     * 「睡觉跳过夜晚」补偿：玩家用床跳过夜晚时，被跳过的那段世界时钟在原版里是直接抹掉的，
     * 作物与熔炉都不会推进。这里按「时间照常流逝」补上。
     */
    private void compensateSleepSkip(MinecraftServer server, long skippedMs) {
        int chunks = enqueueAroundPlayers(server, skippedMs);
        if (chunks > 0) {
            WorldLovesYouMod.LOGGER.info(
                    "[世界爱你] 离线追赶：检测到睡觉跳过夜晚 {}，补偿玩家周围 {} 个区块",
                    human(skippedMs), chunks);
        }
    }

    /**
     * 把每个玩家周围 {@link #BURST_RADIUS_CHUNKS} 区块内**已加载**的区块与动物按 {@code deltaMs} 入队。
     * 只入队、不碰世界（真正的补偿在 {@code drainChunks} / {@code drainEntities} 里做）。
     *
     * @return 入队的区块数（0 = 周围没有已加载区块，也就没什么可补的）
     */
    private int enqueueAroundPlayers(MinecraftServer server, long deltaMs) {
        if (deltaMs <= 0L) {
            return 0;
        }
        int chunks = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            chunks += enqueueAround(player, deltaMs);
        }
        return chunks;
    }

    /**
     * 自检入口（{@code WLY_SELFTEST=1}）：对单个玩家跑一次「周围入队」，返回入队的区块数。
     * 生产路径走 {@link #enqueueAroundPlayers}，这个方法只为自检能在没有真实玩家时验证通路。
     */
    public static int enqueueAroundForTest(ServerPlayer player, long deltaMs) {
        return enqueueAround(player, deltaMs);
    }

    /** 单个玩家的周围入队。 */
    private static int enqueueAround(ServerPlayer player, long deltaMs) {
        if (deltaMs <= 0L) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        int cx = player.chunkPosition().x();
        int cz = player.chunkPosition().z();
        int chunks = 0;
        for (int dx = -BURST_RADIUS_CHUNKS; dx <= BURST_RADIUS_CHUNKS; dx++) {
            for (int dz = -BURST_RADIUS_CHUNKS; dz <= BURST_RADIUS_CHUNKS; dz++) {
                if (CHUNK_QUEUE.size() >= MAX_QUEUE) {
                    return chunks;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    continue; // 没加载的区块没有「已流逝但未推进」的时间，跳过
                }
                CHUNK_QUEUE.add(new PendingChunk(level, x, z, deltaMs));
                chunks++;
            }
        }
        AABB box = player.getBoundingBox().inflate(BURST_RADIUS_CHUNKS * 16.0);
        for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, box)) {
            if (ENTITY_QUEUE.size() >= MAX_QUEUE) {
                break;
            }
            ENTITY_QUEUE.add(new PendingEntity(level, mob, deltaMs));
        }
        return chunks;
    }

    /** 把毫秒数说成人话（日志用）。 */
    private static String human(long ms) {
        if (ms >= 3600_000L) {
            return (ms / 3600_000L) + " 小时";
        }
        if (ms >= 60_000L) {
            return (ms / 60_000L) + " 分钟";
        }
        return (ms / 1000L) + " 秒";
    }

    /** 心跳：即使玩家强杀进程，也留下一个「最近还活着」的时间基准。 */
    private void heartbeat(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeatMs < HEARTBEAT_MS) {
            return;
        }
        writeStamp(server, now);
    }

    private void drainChunks(long deadline) {
        int processed = 0;
        while (processed < MAX_CHUNKS_PER_TICK) {
            PendingChunk pc = CHUNK_QUEUE.poll();
            if (pc == null) {
                return;
            }
            processed++;
            LevelChunk chunk = pc.level().getChunkSource().getChunkNow(pc.x(), pc.z());
            if (chunk != null) {
                try {
                    CatchupEngine.Result r = CatchupEngine.catchupChunk(pc.level(), chunk, pc.deltaMs());
                    if (r.any()) {
                        statChunks++;
                        statFurnaces += r.furnaces();
                        statCampfires += r.campfires();
                        statGrowth += r.growthBlocks();
                        statMisc += r.miscBlocks();
                        statMaxDeltaMs = Math.max(statMaxDeltaMs, pc.deltaMs());
                    }
                } catch (Throwable t) {
                    WorldLovesYouMod.LOGGER.warn("[世界爱你] 离线追赶区块 {} 失败：{}", chunk.getPos(), t.toString());
                }
            }
            // 无论区块是否还在加载，都刷新时间戳：避免「加载→立刻卸载」反复累积 delta。
            touch(pc.level(), pc.x(), pc.z(), System.currentTimeMillis());
            if (System.nanoTime() > deadline) {
                return;
            }
        }
    }

    private void drainEntities(long deadline) {
        int processed = 0;
        while (processed < MAX_ENTITIES_PER_TICK) {
            PendingEntity pe = ENTITY_QUEUE.poll();
            if (pe == null) {
                return;
            }
            processed++;
            AgeableMob mob = pe.mob();
            if (mob.isRemoved() || !mob.isAlive()) {
                continue;
            }
            // 注意单位：AgeableMob.ageUp(int, boolean) 的参数是**秒**，内部 age += 参数 × 20。
            // 传 tick 数会把 1 分钟的离线放大 20 倍（幼年动物瞬间成年）。
            int seconds = (int) Math.min(pe.deltaMs() / 1000L, MAX_AGE_SECONDS);
            if (seconds > 0) {
                try {
                    mob.ageUp(seconds, false);
                    regrowSheepWool(mob);
                    statAnimals++;
                    statMaxDeltaMs = Math.max(statMaxDeltaMs, pe.deltaMs());
                } catch (Throwable t) {
                    WorldLovesYouMod.LOGGER.warn("[世界爱你] 离线追赶实体 {} 失败：{}", mob.getType(), t.toString());
                }
            }
            touch(pe.level(), mob.chunkPosition().x(), mob.chunkPosition().z(), System.currentTimeMillis());
            if (System.nanoTime() > deadline) {
                return;
            }
        }
    }

    /**
     * 羊的羊毛再生：原版要靠吃草，这里按「离线期间照常吃草」补上 ——
     * 被剪毛且脚下是草方块时，草方块变泥土并恢复羊毛。
     */
    private static void regrowSheepWool(AgeableMob mob) {
        if (!(mob instanceof Sheep sheep) || !sheep.isSheared()) {
            return;
        }
        ServerLevel level = (ServerLevel) sheep.level();
        BlockPos below = sheep.blockPosition().below();
        if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
            sheep.setSheared(false);
        }
    }

    /** 批量输出统计（一行代替过去每区块一行）。 */
    private void flushStats(MinecraftServer server, boolean force) {
        if (statChunks == 0L && statAnimals == 0L) {
            return;
        }
        int tick = server.getTickCount();
        boolean idle = CHUNK_QUEUE.isEmpty() && ENTITY_QUEUE.isEmpty();
        if (!force && !idle && tick - statLastFlushTick < STATS_FLUSH_TICKS) {
            return;
        }
        statLastFlushTick = tick;
        WorldLovesYouMod.LOGGER.info(
                "[世界爱你] 离线追赶：补偿 {} 个区块 / {} 只动物（熔炉 {}、营火 {}、生长方块 {}、其它随机刻 {}），最长离线约 {} 分钟",
                statChunks, statAnimals, statFurnaces, statCampfires, statGrowth, statMisc,
                statMaxDeltaMs / 60_000L);
        statChunks = 0L;
        statAnimals = 0L;
        statFurnaces = 0L;
        statCampfires = 0L;
        statGrowth = 0L;
        statMisc = 0L;
        statMaxDeltaMs = 0L;
    }

    /** 启动补偿窗口结束后停用停机时间戳。 */
    private void expireStamp(MinecraftServer server) {
        if (stopStamp == 0L) {
            return;
        }
        long tick = server.getTickCount();
        if (tick < clearStampAtTick) {
            return;
        }
        boolean queueIdle = CHUNK_QUEUE.isEmpty() && ENTITY_QUEUE.isEmpty();
        if (!queueIdle && tick < clearStampAtTick + (STAMP_HARD_LIMIT_TICKS - STAMP_GRACE_TICKS)) {
            return;
        }
        stopStamp = 0L;
        flushStats(server, true);
        WorldLovesYouMod.LOGGER.info("[世界爱你] 离线追赶：启动补偿窗口结束，后续按区块自身时间戳计算");
    }
}
