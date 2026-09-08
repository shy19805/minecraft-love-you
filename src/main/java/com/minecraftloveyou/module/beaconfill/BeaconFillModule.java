package com.minecraftloveyou.module.beaconfill;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 更好的信标放置：手持矿物方块右键信标，自动铺信标基座（最多4层），
 * 潜行+右键连放整组；被替换的非矿物方块掉落回信标顶部。
 * 配置 breakBeaconBaseBlocks / dropReplacedBlockTopBeacon 与原模组同名。
 */
public class BeaconFillModule implements WlyModule {

    public static final String ID = "beacon-fill";
    public static boolean breakBeaconBaseBlocks = true;
    public static boolean dropReplacedBlockTopBeacon = true;
    /** 可作为基座的矿物方块（可扩展注册名白名单）。 */
    public static List<Item> baseItems = new ArrayList<>(List.of(
            Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.DIAMOND_BLOCK,
            Items.EMERALD_BLOCK, Items.NETHERITE_BLOCK));

    public static void register() {
        ModuleManager.register(new BeaconFillModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "更好的信标放置";
    }

    @Override
    public void init(ModuleContext ctx) {
        UseBlockCallback.EVENT.register(this::onUseBlock);
        // 破坏信标连带拆除基座（注册方块破坏后事件，见 BlockBreakEvents）
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!on() || !breakBeaconBaseBlocks || world.isClientSide()) {
                return;
            }
            if (state.getBlock() instanceof BeaconBlock) {
                breakBase(world, pos);
            }
        });
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!on() || world.isClientSide() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BeaconBlock)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !baseItems.contains(held.getItem())) {
            return InteractionResult.PASS;
        }
        boolean sneaking = player.isSecondaryUseActive();
        ServerLevel serverLevel = (ServerLevel) world;
        BlockPos top = pos; // 被替换方块掉落位置（信标顶部）
        boolean placedAny = false;
        for (int layer = 1; layer <= 4; layer++) {
            int n = layer;
            int y = pos.getY() - layer;
            for (int dx = -n; dx <= n; dx++) {
                for (int dz = -n; dz <= n; dz++) {
                    if (held.isEmpty()) {
                        return placedAny ? InteractionResult.SUCCESS : InteractionResult.PASS;
                    }
                    BlockPos target = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                    BlockState targetState = world.getBlockState(target);
                    if (targetState.isAir() || targetState.canBeReplaced()) {
                        serverLevel.setBlockAndUpdate(target, Block.byItem(held.getItem()).defaultBlockState());
                        held.shrink(1);
                        placedAny = true;
                    } else if (isBaseBlock(targetState)) {
                        continue; // 已是矿物块，跳过不消耗
                    } else {
                        // 替换普通方块：被换方块掉回信标顶部（配置）
                        serverLevel.destroyBlock(target, false);
                        if (dropReplacedBlockTopBeacon) {
                            Item oldItem = targetState.getBlock().asItem();
                            if (oldItem != Items.AIR) {
                                Block.popResource(serverLevel, top, new ItemStack(oldItem));
                            }
                        }
                        serverLevel.setBlockAndUpdate(target, Block.byItem(held.getItem()).defaultBlockState());
                        held.shrink(1);
                        placedAny = true;
                    }
                    if (!sneaking) {
                        return InteractionResult.SUCCESS; // 非潜行：一次交互只放一块
                    }
                }
            }
        }
        return placedAny ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean isBaseBlock(BlockState s) {
        return s.getBlock() != net.minecraft.world.level.block.Blocks.AIR
                && baseItems.contains(s.getBlock().asItem());
    }

    /** 拆除信标下方 4 层基座并掉落（掉落位置=信标位置）。 */
    private static void breakBase(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int layer = 1; layer <= 4; layer++) {
            int n = layer;
            int y = pos.getY() - layer;
            for (int dx = -n; dx <= n; dx++) {
                for (int dz = -n; dz <= n; dz++) {
                    BlockPos target = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                    BlockState ts = world.getBlockState(target);
                    if (isBaseBlock(ts)) {
                        Block.dropResources(ts, serverLevel, target, serverLevel.getBlockEntity(target), null, ItemStack.EMPTY);
                        serverLevel.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
}