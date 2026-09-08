package com.minecraftloveyou.module.tamesafety;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 驯服生物避险（每 10 tick 扫描玩家 32 格内已驯服生物）：
 * - 在岩浆/火/仙人掌/甜浆果丛/凋零玫瑰上 → 传送到最近安全地面
 * - 水中氧气不足 → 传送到头顶最近水面
 * - 即将走向危险方块 → 反向轻推
 */
public class TameSafetyModule implements WlyModule {

    public static final String ID = "tame-safety";
    private static int tick = 0;

    public static void register() {
        ModuleManager.register(new TameSafetyModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "驯服生物避险";
    }

    @Override
    public void init(ModuleContext ctx) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!on()) {
                return;
            }
            tick++;
            if (tick % 10 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerLevel level = (ServerLevel) player.level();
                AABB box = player.getBoundingBox().inflate(32, 16, 32);
                List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, box, TamableAnimal::isTame);
                for (TamableAnimal pet : pets) {
                    protectPet(pet, level);
                }
            }
        });
    }

    private void protectPet(TamableAnimal pet, ServerLevel level) {
        BlockPos feet = pet.blockPosition();
        BlockState inState = level.getBlockState(feet);
        BlockState below = level.getBlockState(feet.below());

        boolean inLava = inState.liquid() && !level.getFluidState(feet).is(FluidTags.WATER);
        boolean onHazard = isHazardBlock(below) || isHazardBlock(inState);
        boolean drowning = pet.isEyeInFluid(FluidTags.WATER) && pet.getAirSupply() < pet.getMaxAirSupply() * 3 / 10;

        if (inLava || onHazard || drowning) {
            BlockPos safe = findSafeNear(level, pet.blockPosition(), 8);
            if (safe != null) {
                pet.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                pet.setDeltaMovement(Vec3.ZERO);
                pet.hurtMarked = true;
            }
            return;
        }

        // 预判躲避：前方路径扫 2.5 格（覆盖正在蔓延的岩浆/火焰与相邻危险方块）
        Vec3 dir = pet.getDeltaMovement();
        for (int step = 1; step <= 5; step++) {
            double f = step * 1.5;
            BlockPos ahead = BlockPos.containing(pet.getX() + dir.x * f, pet.getY() + 0.5, pet.getZ() + dir.z * f);
            boolean aheadHazard = isHazardBlock(level.getBlockState(ahead))
                    || isHazardBlock(level.getBlockState(ahead.below()))
                    || isSpreadingFireNear(level, ahead);
            if (aheadHazard) {
                if (step == 1) {
                    // 已贴脸：反向拉回
                    pet.setDeltaMovement(-dir.x * 0.8, dir.y, -dir.z * 0.8);
                } else {
                    // 尚有距离：横向偏转绕行
                    double px = -dir.z, pz = dir.x;
                    pet.setDeltaMovement(dir.x * 0.3 + px * 0.6, dir.y, dir.z * 0.3 + pz * 0.6);
                }
                pet.hurtMarked = true;
                break;
            }
        }
    }

    /** 蔓延探测：目标点周围 1 格有无 火/岩浆（含上方，火焰会往上烧）。 */
    private boolean isSpreadingFireNear(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState s = level.getBlockState(pos.offset(dx, dy, dz));
                    if (s.is(net.minecraft.world.level.block.Blocks.FIRE)
                            || s.is(net.minecraft.world.level.block.Blocks.LAVA)
                            || level.getFluidState(pos.offset(dx, dy, dz)).is(net.minecraft.tags.FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isHazardBlock(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.LAVA || b == Blocks.FIRE || b == Blocks.CACTUS || b == Blocks.SWEET_BERRY_BUSH
                || b == Blocks.WITHER_ROSE || b == Blocks.MAGMA_BLOCK || b == Blocks.POINTED_DRIPSTONE
                || b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE;
    }

    /** 找附近安全地面（非危险、上方两格空气）。 */
    private BlockPos findSafeNear(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState ground = level.getBlockState(p.below());
                    BlockState at = level.getBlockState(p);
                    BlockState above = level.getBlockState(p.above());
                    if (ground.isSolidRender()
                            && at.isAir() && above.isAir()
                            && !isHazardBlock(ground)) {
                        double d = p.distSqr(center);
                        if (d < bestDist) {
                            bestDist = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }
}