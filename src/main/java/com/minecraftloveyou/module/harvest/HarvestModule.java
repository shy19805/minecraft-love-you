package com.minecraftloveyou.module.harvest;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 右键收获并补种（连锁增强版）：
 * 1) 右键成熟作物 → 收获+补种 + 连锁周围 chainLimit 格（默认64，可调）同种（可改"所有"）作物一并收获补种；
 * 2) 手持骨粉右键作物 → 连锁催熟周围 chainLimit 格作物；
 * 3) 手持种子右键耕地 → 连锁种植周围 plantLimit 格（默认64）空闲耕地。
 */
public class HarvestModule implements WlyModule {

    public static final String ID = "harvest";
    public static boolean requireHoe = false;
    public static boolean damageOnHarvest = true;
    public static boolean expOnHarvest = false;
    public static boolean playSound = true;
    public static int chainLimit = 64;
    public static boolean sameSpecies = true;
    public static int plantLimit = 64;
    public static final List<String> cropWhitelist = new ArrayList<>();

    public static void register() {
        ModuleManager.register(new HarvestModule());
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
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!on() || player.isSpectator() || world.isClientSide()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        ServerLevel serverLevel = (ServerLevel) world;
        ItemStack held = player.getItemInHand(hand);
        // 连锁开关：客户端按住"作物连锁"键经 C2S 包同步；未按住 = 单格行为
        boolean chainActive = player instanceof net.minecraft.server.level.ServerPlayer sp
                && com.minecraftloveyou.core.WlyNet.chainHeld(sp);

        // 分支 2：骨粉催熟（按住连锁键 → 连锁催熟；否则单格催熟）
        if (held.is(Items.BONE_MEAL) && block instanceof CropBlock targetCrop) {
            if (chainActive) {
                return chainBonemeal(serverLevel, player, pos, targetCrop, held);
            }
            // 单格：原版骨粉逻辑放行
            return InteractionResult.PASS;
        }
        // 分支 3：手持种子 → 目标是耕地 → 连锁种植（仅按住连锁键）
        if (state.getBlock() instanceof FarmlandBlock && isSeedItem(held)) {
            if (chainActive) {
                return chainPlant(serverLevel, player, pos, held);
            }
            return InteractionResult.PASS; // 未按连锁键：放行原版种植逻辑
        }
        // 分支 1：右键收获（按住连锁键 → 连锁收获；否则单格收获+补种）
        if (isCropBlock(block)) {
            if (!isMature(block, state) || !whitelistOk(block)) {
                return InteractionResult.PASS;
            }
            if (requireHoe
                    && !player.getMainHandItem().is(ItemTags.HOES)
                    && !player.getOffhandItem().is(ItemTags.HOES)) {
                return InteractionResult.PASS;
            }
            if (chainActive) {
                List<BlockPos> chain = findCropChain(serverLevel, pos, block, sameSpecies, chainLimit,
                        p -> {
                            BlockState ps = serverLevel.getBlockState(p);
                            return isMature(ps.getBlock(), ps); // 必须用各自方块的类型取 AGE
                        });
                com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[世界爱你][连锁] 收获链 {} 株（同种={}, 上限={}）", chain.size(), sameSpecies, chainLimit);
                int harvested = 0;
                for (BlockPos p : chain) {
                    harvestOne(serverLevel, player, p, serverLevel.getBlockState(p), hand);
                    harvested++;
                }
                return harvested > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            // 单格收获+补种
            harvestOne(serverLevel, player, pos, state, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * 以目标点为中心的半径扫描（水平 radius、垂直 ±1），收集目标作物。
     * 不依赖"作物相邻"假设，收获/催熟都稳定工作；扫描规模固定 ~243 格，无卡死风险。
     */
    private static List<BlockPos> findCropChain(ServerLevel level, BlockPos start, Block startBlock,
                                                boolean sameSpeciesOnly, int limit,
                                                java.util.function.Predicate<BlockPos> filter) {
        List<BlockPos> result = new ArrayList<>();
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (result.size() >= limit) {
                        return result;
                    }
                    BlockPos p = start.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    boolean speciesOk = sameSpeciesOnly ? st.getBlock() == startBlock : isCropBlock(st.getBlock());
                    if (!speciesOk) {
                        continue;
                    }
                    // 防御：非同类属性误取会抛异常（如甜浆果丛类），吞掉按不匹配处理
                    boolean ok;
                    try {
                        ok = filter.test(p);
                    } catch (Exception e) {
                        continue;
                    }
                    if (!ok) {
                        continue;
                    }
                    result.add(p);
                }
            }
        }
        // 收获模式：filter 已限成熟；催熟模式：filter 限未成熟
        return result;
    }

    /** 未成熟判定（催熟扫描用）。 */
    private static boolean isImmature(Block block, BlockState state) {
        if (block instanceof CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE;
        }
        return false;
    }

    private void harvestOne(ServerLevel level, Player player, BlockPos pos, BlockState state, InteractionHand hand) {
        Block block = state.getBlock();
        ItemStack tool = player.getMainHandItem();
        Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, tool);
        // 注意：不能用 block.asItem() 当种子 —— 小麦方块 asItem 是「小麦」而非「小麦种子」，
        // 甜菜同理。必须走原版 CropBlock.getBaseSeedId()（见 mixin/CropBlockInvoker）。
        boolean hasSeed = consumeSeed(player, seedFor(block));
        BlockState replant = replantState(block, state);
        if (hasSeed && replant != null) {
            level.setBlock(pos, replant, 3);
        } else {
            level.removeBlock(pos, false);
        }
        if (playSound) {
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (expOnHarvest) {
            ExperienceOrb.award(level, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 1);
        }
        if (damageOnHarvest && tool.is(ItemTags.HOES) && !tool.isEmpty()) {
            tool.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    /** 骨粉连锁催熟：BFS 未成熟作物逐个催熟（每格消耗 1 骨粉按 vanilla growCrop 概率；失败兜底直接满龄）。 */
    private InteractionResult chainBonemeal(ServerLevel level, Player player, BlockPos pos, CropBlock crop, ItemStack held) {
        if (!whitelistOk(crop)) {
            return InteractionResult.PASS;
        }
        List<BlockPos> chain = findCropChain(level, pos, crop, sameSpecies, chainLimit,
                p -> {
                    BlockState s = level.getBlockState(p);
                    return s.getBlock() instanceof CropBlock c && !c.isMaxAge(s);
                });
        com.minecraftloveyou.WorldLovesYouMod.LOGGER.info("[世界爱你][连锁] 催熟链 {} 株", chain.size());
        int grown = 0;
        for (BlockPos p : chain) {
            if (held.isEmpty() && !player.getAbilities().instabuild) {
                break;
            }
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof CropBlock c) || c.isMaxAge(s)) {
                continue;
            }
            if (player.getAbilities().instabuild) {
                level.setBlock(p, c.getStateForAge(c.getMaxAge()), 3);
                grown++;
            } else if (BoneMealItem.growCrop(held, level, p)) {
                held.shrink(1);
                grown++;
            }
        }
        return grown > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** 连锁种植：BFS 相连空闲耕地（上方空气）逐格种下（种子不足即停）。maxNodes 硬上限防卡死。 */
    private InteractionResult chainPlant(ServerLevel level, Player player, BlockPos pos, ItemStack seed) {
        List<BlockPos> spots = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);
        visited.add(pos);
        int maxNodes = Math.max(256, plantLimit * 4); // 防卡死硬上限
        while (!queue.isEmpty() && spots.size() < plantLimit && visited.size() <= maxNodes) {
            BlockPos cur = queue.poll();
            if (level.getBlockState(cur).getBlock() instanceof FarmlandBlock && level.getBlockState(cur.above()).isAir()) {
                spots.add(cur);
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos next = cur.relative(d);
                if (visited.size() >= maxNodes) {
                    break;
                }
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        CropBlock mainCrop = cropForSeed(seed);
        int planted = 0;
        for (BlockPos farm : spots) {
            if (seed.isEmpty() && !player.getAbilities().instabuild) {
                break;
            }
            BlockPos above = farm.above();
            BlockState newState;
            if (seed.is(Items.NETHER_WART)) {
                newState = net.minecraft.world.level.block.Blocks.NETHER_WART.defaultBlockState();
            } else if (mainCrop != null) {
                newState = mainCrop.defaultBlockState();
            } else {
                break;
            }
            level.setBlock(above, newState, 3);
            if (!player.getAbilities().instabuild) {
                seed.shrink(1);
            }
            planted++;
            if (playSound) {
                level.playSound(null, above, newState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return planted > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private CropBlock cropForSeed(ItemStack seed) {
        if (seed.is(Items.WHEAT_SEEDS)) {
            return (CropBlock) net.minecraft.world.level.block.Blocks.WHEAT;
        }
        if (seed.is(Items.CARROT)) {
            return (CropBlock) net.minecraft.world.level.block.Blocks.CARROTS;
        }
        if (seed.is(Items.POTATO)) {
            return (CropBlock) net.minecraft.world.level.block.Blocks.POTATOES;
        }
        if (seed.is(Items.BEETROOT_SEEDS)) {
            return (CropBlock) net.minecraft.world.level.block.Blocks.BEETROOTS;
        }
        return null;
    }

    private boolean isSeedItem(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.CARROT) || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.NETHER_WART);
    }

    private static boolean isCropBlock(Block b) {
        return b instanceof CropBlock || b instanceof NetherWartBlock || b instanceof CocoaBlock;
    }

    private static boolean isMature(Block block, BlockState state) {
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        return false;
    }

    private static BlockState replantState(Block block, BlockState state) {
        if (block instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        if (block instanceof NetherWartBlock) {
            return block.defaultBlockState();
        }
        if (block instanceof CocoaBlock) {
            return state.setValue(CocoaBlock.AGE, 0);
        }
        return null;
    }

    private boolean whitelistOk(Block block) {
        return cropWhitelist.isEmpty()
                || cropWhitelist.contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    private static boolean consumeSeed(Player player, ItemStack seed) {
        // 创造模式不消耗（种植分支已有同样判定，补种这条漏了）
        if (player.getAbilities().instabuild) {
            return true;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, seed) && stack.getCount() > 0) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * 该作物「补种」需要消耗的物品。
     *
     * <p>关键：小麦方块的 {@code asItem()} 是**小麦**而不是**小麦种子**，甜菜同理；
     * 直接用 {@code block.asItem()} 会导致小麦/甜菜永远找不到种子、只收不补种。
     * 原版记录种子的是 {@link CropBlock#getBaseSeedId()}（protected）→ 走 Invoker。
     */
    private static ItemStack seedFor(Block block) {
        if (block instanceof CropBlock crop) {
            try {
                return new ItemStack(((com.minecraftloveyou.mixin.CropBlockInvoker) crop).wly$getBaseSeedId());
            } catch (Throwable ignored) {
                // Invoker 未生效（理论上不会）：退回原版 asItem
            }
        }
        if (block instanceof NetherWartBlock) {
            return new ItemStack(Items.NETHER_WART);
        }
        if (block instanceof CocoaBlock) {
            return new ItemStack(Items.COCOA_BEANS);
        }
        return new ItemStack(block.asItem());
    }
}