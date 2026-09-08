package com.minecraftloveyou.module.harvest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作物连锁工具：从起点 BFS 搜索相邻作物（26 方向含对角与上下 1 格），
 * 上限 limit 可配；匹配模式：同种 / 所有作物。
 */
public class ChainMiner {

    /** BFS 搜索与 start 相连的作物方块（含 start）。 */
    public static List<BlockPos> findChain(ServerLevel level, BlockPos start, Block startBlock,
                                           boolean sameSpeciesOnly, int limit,
                                           java.util.function.Predicate<BlockPos> extraFilter) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos cur = queue.poll();
            BlockState state = level.getBlockState(cur);
            if (!matches(state, startBlock, sameSpeciesOnly)) {
                continue;
            }
            if (extraFilter != null && !extraFilter.test(cur)) {
                continue;
            }
            result.add(cur);
            if (result.size() >= limit) {
                break;
            }
            for (Direction d : Direction.values()) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = cur.relative(d).offset(0, dy, 0);
                    if (visited.add(next) && result.size() + queue.size() < limit * 4) {
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }

    private static boolean matches(BlockState state, Block startBlock, boolean sameSpeciesOnly) {
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop)) {
            return false;
        }
        if (sameSpeciesOnly) {
            return state.getBlock() == startBlock;
        }
        return true;
    }

    /** 掉落收集（供连锁收获统计）。 */
    public static void dropAt(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        Block.dropResources(state, level, pos, level.getBlockEntity(pos), null, tool);
    }
}