package com.minecraftloveyou.module.offlinecatchup;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.mixin.CatchupFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 离线追赶的实际补偿逻辑：全部调用原版逻辑，不做数值模拟。
 *
 * <p><b>调用时机（关键）</b>：本类**只能**在服务器 tick（{@code END_SERVER_TICK}）里被调用，
 * <b>绝不能在 {@code ServerChunkEvents.CHUNK_LOAD} 回调里直接调用</b>。
 * 原因：熔炉 {@code serverTick} 内部会执行 {@code ServerLevel.setBlock(pos, state, 3)}
 * （flag 3 = 更新邻居 + 同步客户端），而 {@code CHUNK_LOAD} 是在区块仍被
 * {@code ChunkMap} 装载的过程中触发的；此时写方块触发邻居更新会去取相邻区块，
 * 走到 {@code ServerChunkCache.getChunk(...).join()} —— 重入同一个装载流程 →
 * **死锁**，表现为「卡在加载地形中」。
 *
 * <p>补偿内容：
 * <ul>
 *   <li><b>熔炉族</b>（熔炉/高炉/烟熏炉）：循环 {@link AbstractFurnaceBlockEntity#serverTick} 推进；</li>
 *   <li><b>营火</b>：循环 {@link CampfireBlockEntity#cookTick} 推进；</li>
 *   <li><b>随机刻</b>：按「随机刻期望」折算每个可随机刻方块应被选中的次数，
 *       直接调用原版 {@code BlockState.randomTick}。分两档：
 *       <ul>
 *         <li><b>生长档</b>（带 {@code age}/{@code stage} 属性的方块：作物、树苗、茎、甘蔗、
 *             仙人掌、竹子、浆果、下界疣、可可、藤蔓、垂泪藤/海带等）上限
 *             {@link #MAX_GROWTH_TICKS_PER_BLOCK} —— 这一档必须够大，否则离线几小时后
 *             作物仍然看不出变化（小麦每级约需 26 次随机刻，长熟约需 180+ 次）；</li>
 *         <li><b>其它档</b>（草方块蔓延、雪、冰、树叶衰减等，数量极多）上限
 *             {@link #MAX_RANDOM_TICKS_PER_BLOCK}，避免拖垮区块加载。</li>
 *       </ul>
 *       {@link BaseFireBlock} 单独排除：火也有 {@code age} 属性，但按离线时间疯狂蔓延会烧掉整片森林。</li>
 * </ul>
 * 每个区块有 {@link #TIME_BUDGET_NS} 的时间预算，避免大量机器/农田区块造成明显卡顿。
 */
public final class CatchupEngine {

    /** 单区块单次补偿的时间预算（纳秒）。 */
    private static final long TIME_BUDGET_NS = 20_000_000L;

    /** 生长档：单个方块一次补偿最多补多少个随机刻。 */
    private static final int MAX_GROWTH_TICKS_PER_BLOCK = 256;

    /** 其它档：单个方块一次补偿最多补多少个随机刻。 */
    private static final int MAX_RANDOM_TICKS_PER_BLOCK = 16;

    /** 原版随机刻模型：每 tick 每个 16³ 段从 4096 个位置里选 randomTickSpeed 个。 */
    private static final double SECTION_VOLUME = 16.0 * 16.0 * 16.0;

    /** 营火单次补偿的 tick 上限（1 小时）。 */
    private static final long MAX_CAMPFIRE_TICKS = 72_000L;

    /** 营火烹饪配方查询器（懒加载，全局一份）。 */
    private static RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> campfireCheck;

    private CatchupEngine() {
    }

    /** 单区块补偿结果。 */
    public record Result(int furnaces, int campfires, int growthBlocks, int miscBlocks) {
        public static final Result ZERO = new Result(0, 0, 0, 0);

        public boolean any() {
            return furnaces > 0 || campfires > 0 || growthBlocks > 0 || miscBlocks > 0;
        }

        public Result plus(Result o) {
            return new Result(furnaces + o.furnaces, campfires + o.campfires,
                    growthBlocks + o.growthBlocks, miscBlocks + o.miscBlocks);
        }
    }

    /**
     * 对一个已加载区块做离线追赶。
     *
     * @param level   服务端世界
     * @param chunk   已完整加载的区块（必须是 {@code getChunkNow} 拿到的 {@link LevelChunk}）
     * @param deltaMs 距离上次活跃的现实毫秒数
     */
    public static Result catchupChunk(ServerLevel level, LevelChunk chunk, long deltaMs) {
        long ticks = deltaMs / 50L;
        if (ticks <= 0) {
            return Result.ZERO;
        }
        long deadline = System.nanoTime() + TIME_BUDGET_NS;
        int furnaces = catchupFurnaces(level, chunk, ticks, deadline);
        int campfires = catchupCampfires(level, chunk, ticks, deadline);
        int[] blocks = catchupRandomTicks(level, chunk, ticks, deadline);
        return new Result(furnaces, campfires, blocks[0], blocks[1]);
    }

    // ───────────────────────────── 熔炉族 ─────────────────────────────

    private static int catchupFurnaces(ServerLevel level, LevelChunk chunk, long ticks, long deadline) {
        long budget = Math.min(ticks, OfflineCatchupModule.MAX_FURNACE_TICKS);
        if (budget <= 0) {
            return 0;
        }
        int count = 0;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            BlockEntity be = chunk.getBlockEntity(pos);
            if (!(be instanceof AbstractFurnaceBlockEntity furnace)) {
                continue;
            }
            BlockState state = chunk.getBlockState(pos);
            int idle = 0;
            for (long i = 0; i < budget; i++) {
                if (furnace.isRemoved()) {
                    break;
                }
                AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
                if (((CatchupFurnaceAccessor) furnace).wly$litTimeRemaining() > 0) {
                    idle = 0;
                } else if (++idle >= 2) {
                    break; // 已经没有燃料在烧，后续 tick 不会再有进展
                }
                if ((i & 0xFFL) == 0xFFL && System.nanoTime() > deadline) {
                    break; // 超出时间预算：本区块补偿到此为止
                }
            }
            count++;
        }
        return count;
    }

    // ───────────────────────────── 营火 ─────────────────────────────

    private static int catchupCampfires(ServerLevel level, LevelChunk chunk, long ticks, long deadline) {
        long budget = Math.min(ticks, MAX_CAMPFIRE_TICKS);
        int count = 0;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            BlockEntity be = chunk.getBlockEntity(pos);
            if (!(be instanceof CampfireBlockEntity campfire)) {
                continue;
            }
            if (isEmptyCampfire(campfire)) {
                continue; // 没有在烤的东西，直接跳过（营火 tick 是 600 刻/个，不空转会很快收敛）
            }
            BlockState state = chunk.getBlockState(pos);
            for (long i = 0; i < budget; i++) {
                if (campfire.isRemoved()) {
                    break;
                }
                CampfireBlockEntity.cookTick(level, pos, state, campfire, campfireCheck());
                if (isEmptyCampfire(campfire)) {
                    break;
                }
                if ((i & 0xFFL) == 0xFFL && System.nanoTime() > deadline) {
                    break;
                }
            }
            count++;
        }
        return count;
    }

    private static boolean isEmptyCampfire(CampfireBlockEntity campfire) {
        var items = campfire.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> campfireCheck() {
        if (campfireCheck == null) {
            campfireCheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
        }
        return campfireCheck;
    }

    // ───────────────────────────── 通用随机刻 ─────────────────────────────

    /**
     * 通用随机刻补偿：对每个「可随机刻」的方块，按期望次数调用原版
     * {@code BlockState.randomTick}。
     *
     * <p>原版模型：每 tick，每个 16³ 段从 4096 个位置里随机选 {@code randomTickSpeed}
     * 个位置执行随机刻。所以某个方块在 {@code ticks} 刻内被选中的期望次数
     * = {@code ticks × randomTickSpeed / 4096}。
     *
     * @return {@code [生长档方块数, 其它档方块数]}
     */
    private static int[] catchupRandomTicks(ServerLevel level, LevelChunk chunk, long ticks, long deadline) {
        int speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        if (speed <= 0) {
            return new int[]{0, 0};
        }
        double expected = (double) ticks * (double) speed / SECTION_VOLUME;
        int growthTicks = (int) Math.min(MAX_GROWTH_TICKS_PER_BLOCK, Math.round(expected));
        int miscTicks = (int) Math.min(MAX_RANDOM_TICKS_PER_BLOCK, Math.round(expected));
        if (growthTicks <= 0 && miscTicks <= 0) {
            return new int[]{0, 0};
        }

        int minSectionY = level.getMinY() >> 4;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        int growth = 0;
        int misc = 0;

        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int sectionBaseY = (minSectionY + index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.isRandomlyTicking()) {
                            continue; // 绝大多数方块在这里被跳过
                        }
                        boolean isGrowth = isGrowthLike(state);
                        int times = isGrowth ? growthTicks : miscTicks;
                        if (times <= 0) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(baseX + x, sectionBaseY + y, baseZ + z);
                        for (int i = 0; i < times; i++) {
                            state.randomTick(level, pos, level.getRandom());
                            state = section.getBlockState(x, y, z);
                            if (!state.isRandomlyTicking()) {
                                break; // 已经变成不可随机刻的方块（例如作物长成、雪融化）
                            }
                        }
                        if (isGrowth) {
                            growth++;
                        } else {
                            misc++;
                        }
                        if (((growth + misc) & 0x7) == 0 && System.nanoTime() > deadline) {
                            return new int[]{growth, misc}; // 超出时间预算
                        }
                    }
                }
            }
        }
        return new int[]{growth, misc};
    }

    /**
     * 「生长型」方块判定：带 {@code age} / {@code stage} / {@code age_N} 属性的方块。
     *
     * <p>用属性名判定而不是用类判定，是为了同时覆盖原版（小麦/胡萝卜/土豆/甜菜、
     * 树苗、南瓜茎、西瓜茎、甘蔗、仙人掌、竹子、甜浆果、下界疣、可可、
     * 藤蔓、海带/垂泪藤/缠怨藤、紫颂花）与模组方块（例如植物大战僵尸的植物）。
     *
     * <p>火（{@link BaseFireBlock}）也有 {@code age} 属性，必须排除：
     * 否则离线 8 小时回来会把整片森林按原版火焰蔓延速度烧干净。
     */
    private static boolean isGrowthLike(BlockState state) {
        if (state.getBlock() instanceof BaseFireBlock) {
            return false;
        }
        if (state.getBlock() instanceof CropBlock) {
            return true;
        }
        for (Property<?> property : state.getProperties()) {
            String name = property.getName();
            if (name.equals("age") || name.equals("stage") || name.startsWith("age_")) {
                return true;
            }
        }
        return false;
    }
}
