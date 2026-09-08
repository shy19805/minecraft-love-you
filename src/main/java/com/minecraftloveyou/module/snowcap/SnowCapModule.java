package com.minecraftloveyou.module.snowcap;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Random;

/**
 * 雪覆盖（snow-cap）：下雨/下雪时，玩家周围露天方块顶部逐层积雪（每层 1/8 高，最多 8 层）。
 * 温暖生物群系不积雪；逐层累积更像真实降雪。
 */
public class SnowCapModule implements WlyModule {

    public static final String ID = "snow-cap";
    private static int tick = 0;
    private static final Random RANDOM = new Random();

    public static void register() {
        ModuleManager.register(new SnowCapModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "雨雪覆盖积雪";
    }

    @Override
    public void init(ModuleContext ctx) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!on() || tick++ % 100 != 0) { // 每 5 秒一次
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerLevel level = (ServerLevel) player.level();
                if (!level.isRaining() && !level.isThundering()) {
                    continue;
                }
                // 逐个玩家周围随机采样 24 个位置（性能友好）
                BlockPos base = player.blockPosition();
                for (int i = 0; i < 24; i++) {
                    int dx = RANDOM.nextInt(33) - 16;
                    int dz = RANDOM.nextInt(33) - 16;
                    int dy = RANDOM.nextInt(9) - 4;
                    BlockPos pos = base.offset(dx, dy, dz);
                    trySnowAt(level, pos);
                }
            }
        });
    }

    private static void trySnowAt(ServerLevel level, BlockPos pos) {
        // 该位置上方是否露天 + 该格是否可积雪（空气/已有雪层）
        BlockPos target = pos.above();
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(target);
        // 天气可见性：该点能看见天空
        if (!level.canSeeSky(pos)) {
            return;
        }
        // 温暖生物群系不积雪
        var biome = level.getBiome(pos);
        if (biome.value().warmEnoughToRain(pos, 0)) {
            return;
        }
        if (above.isAir() && state.isSolidRender()) {
            // 顶面实心 → 放 1 层雪
            level.setBlock(target, Blocks.SNOW.defaultBlockState(), 3);
            level.gameEvent(null, GameEvent.BLOCK_PLACE, target);
        } else if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) < 8) {
            // 已有雪 → 加一层
            level.setBlock(target, above.setValue(SnowLayerBlock.LAYERS, above.getValue(SnowLayerBlock.LAYERS) + 1), 3);
        }
    }
}