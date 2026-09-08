package com.minecraftloveyou.module.vineextend;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.TwistingVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 藤蔓/梯子延伸：手持 垂泪藤/缠怨藤/藤蔓/梯子
 * - 抬头(仰视)对准同种方块 → 在其上方放置（垂泪藤原生向下、这里允许向上）
 * - 低头(俯视)对准同种方块 → 在其下方放置
 * 梯子会继承朝向；藤蔓复制原方块属性。
 */
public class VineExtendModule implements WlyModule {

    public static final String ID = "vine-extend";

    public static void register() {
        ModuleManager.register(new VineExtendModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "藤蔓/梯子延伸";
    }

    @Override
    public void init(ModuleContext ctx) {
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!on() || player.isSpectator() || world.isClientSide()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        Block heldBlock = heldBlockFor(held);
        if (heldBlock == null) {
            return InteractionResult.PASS;
        }
        BlockState target = world.getBlockState(hit.getBlockPos());
        if (target.getBlock() != heldBlock) {
            return InteractionResult.PASS; // 必须对准同种方块
        }
        // 仰视 → 上方；俯视 → 下方（水平注视不给处理）
        float pitch = player.getXRot();
        boolean up = pitch < -35.0F;
        boolean down = pitch > 35.0F;
        if (up == down) {
            return InteractionResult.PASS;
        }
        // 追链：沿方向一直找同种方块，直到第一个非同种/空气位 = 放置点（支持 2/3/…格续放）
        BlockPos dest = hit.getBlockPos();
        while (world.getBlockState(up ? dest.above() : dest.below()).getBlock() == heldBlock) {
            dest = up ? dest.above() : dest.below();
        }
        dest = up ? dest.above() : dest.below();
        ServerLevel serverLevel = (ServerLevel) world;
        BlockState at = world.getBlockState(dest);
        if (!at.isAir() && !at.canBeReplaced()) {
            return InteractionResult.PASS;
        }
        BlockState newState = stateFor(heldBlock, target, up);
        // flag 2|16: 不触发邻接更新 → canSurvive 不会把无支撑藤蔓打掉，实现无限延伸
        serverLevel.setBlock(dest, newState, 2 | 16);
        world.playSound(null, dest, newState.getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private Block heldBlockFor(ItemStack held) {
        if (held.is(Items.LADDER)) {
            return Blocks.LADDER;
        }
        if (held.is(Items.VINE)) {
            return Blocks.VINE;
        }
        if (held.is(Items.WEEPING_VINES)) {
            return Blocks.WEEPING_VINES;
        }
        if (held.is(Items.TWISTING_VINES)) {
            return Blocks.TWISTING_VINES;
        }
        return null;
    }

    /** 目标位置状态：梯子继承水平朝向；藤蔓复制原属性。 */
    private BlockState stateFor(Block heldBlock, BlockState source, boolean up) {
        BlockState s = heldBlock.defaultBlockState();
        if (heldBlock instanceof LadderBlock) {
            Direction facing = source.getValue(LadderBlock.FACING);
            s = s.setValue(LadderBlock.FACING, facing);
        } else if (heldBlock instanceof VineBlock) {
            // 复制原藤蔓各方向连接；上下自动成立
            for (Direction d : Direction.Plane.HORIZONTAL) {
                s = s.setValue(VineBlock.getPropertyForFace(d), source.getValue(VineBlock.getPropertyForFace(d)));
            }
        } else if (heldBlock instanceof WeepingVinesBlock || heldBlock instanceof TwistingVinesBlock) {
            // 年龄复制
            if (source.hasProperty(BlockStateProperties.AGE_25)) {
                s = s.setValue(BlockStateProperties.AGE_25, source.getValue(BlockStateProperties.AGE_25));
            }
        }
        return s;
    }
}