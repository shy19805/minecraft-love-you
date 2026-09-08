package com.minecraftloveyou.module.clickthrough;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 点击穿透：右键命中告示牌（任意朝向）时，沿视线继续 raytrace 跳过告示牌，
 * 命中后方方块后**手动触发其右键逻辑**（PASS 不会自动选中后方方块，必须代为触发）。
 */
public class ClickthroughModule implements WlyModule {

    public static final String ID = "clickthrough";

    public static void register() {
        ModuleManager.register(new ClickthroughModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "点击穿透";
    }

    @Override
    public void init(ModuleContext ctx) {
        ItemFrameClickthrough.register();
        UseBlockCallback.EVENT.register(ClickthroughModule::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        // shift=正常使用告示牌（不穿透）
        if (!WlyModule.on(ID) || world.isClientSide() || !(player instanceof ServerPlayer sp)
                || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        BlockState state = world.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof SignBlock)) {
            return InteractionResult.PASS;
        }
        // 沿视线继续，跳过告示牌（含路径上的一切告示牌）
        Vec3 eye = sp.getEyePosition();
        Vec3 dir = sp.getViewVector(1.0F);
        BlockHitResult next = pickSkippingSigns(world, sp, eye, dir, 8);
        if (next == null || next.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos behindPos = next.getBlockPos();
        if (behindPos.equals(hit.getBlockPos())) {
            return InteractionResult.PASS;
        }
        BlockState behindState = world.getBlockState(behindPos);
        // 用玩家当前手持物品对后方方块执行 use
        InteractionResult result = behindState.useItemOn(sp.getItemInHand(hand), world, sp, hand, next);
        if (result.consumesAction()) {
            return result;
        }
        return behindState.useWithoutItem(world, sp, next);
    }

    /** 递归 raytrace，跳过全部告示牌类方块。 */
    private static BlockHitResult pickSkippingSigns(Level world, ServerPlayer sp, Vec3 eye, Vec3 dir, int depth) {
        if (depth <= 0) {
            return null;
        }
        var ctx = new ClipContext(eye, eye.add(dir.scale(8)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp);
        BlockHitResult hit = world.clip(ctx);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        if (world.getBlockState(hit.getBlockPos()).getBlock() instanceof SignBlock) {
            // 从告示牌后方一点继续
            Vec3 continueFrom = hit.getLocation().add(dir.scale(0.06));
            return pickSkippingSigns(world, sp, continueFrom, dir, depth - 1);
        }
        return hit;
    }
}
