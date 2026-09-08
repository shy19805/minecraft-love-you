package com.minecraftloveyou.module.smoothbedrock;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/** 平滑基岩：生成完成后（CHUNK_GENERATE，安全时机）主世界基岩带压平为单层。 */
public class SmoothBedrockModule implements WlyModule {

    public static final String ID = "smooth-bedrock";

    public static void register() {
        ModuleManager.register(new SmoothBedrockModule());
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
    public boolean enabledByDefault() {
        return false; // 生成改动型功能，默认关，用户开
    }

    @Override
    public void init(ModuleContext ctx) {
        ServerChunkEvents.CHUNK_GENERATE.register(SmoothBedrockModule::onGenerate);
    }

    /** 生成完成后（线程安全）：主世界基岩带压平为单层。 */
    private static void onGenerate(ServerLevel level, LevelChunk chunk) {
        if (!WlyModule.on(ID)) {
            return;
        }
        // 仅主世界（minY=-64）
        if (level.getMinY() != -64) {
            return;
        }
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        int minY = level.getMinY();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx;
                int z = cp.getMinBlockZ() + lz;
                // 找本列最低基岩
                int bottom = -1;
                for (int y = minY; y <= minY + 10; y++) {
                    bp.set(x, y, z);
                    if (chunk.getBlockState(bp).is(Blocks.BEDROCK)) {
                        bottom = y;
                        break;
                    }
                }
                if (bottom == -1) {
                    // 无底格保底：最底格一定放基岩
                    bp.set(x, minY, z);
                    chunk.setBlockState(bp, Blocks.BEDROCK.defaultBlockState(), 0);
                    continue;
                }
                // 最低基岩之上的基岩改石
                for (int y = bottom + 1; y <= minY + 10; y++) {
                    bp.set(x, y, z);
                    if (chunk.getBlockState(bp).is(Blocks.BEDROCK)) {
                        chunk.setBlockState(bp, Blocks.STONE.defaultBlockState(), 0);
                    }
                }
            }
        }
    }
}
