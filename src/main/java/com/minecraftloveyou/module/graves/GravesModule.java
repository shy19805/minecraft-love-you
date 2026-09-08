package com.minecraftloveyou.module.graves;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * 墓碑：玩家死亡 → 在死亡位置生成墓碑方块（存全部物品+经验）；
 * 右键墓碑取回全部。配置开关：graves。
 */
public class GravesModule implements WlyModule {

    public static final String ID = "graves";
    public static final Identifier GRAVE_ID = Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "grave");
    public static GraveBlock GRAVE_BLOCK;
    public static net.minecraft.world.item.BlockItem GRAVE_ITEM;
    public static BlockEntityType<GraveBlockEntity> GRAVE_BE_TYPE;

    public static void register() {
        registerBlocks(); // 注册必须在 freeze 前尽早完成
        ModuleManager.register(new GravesModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "墓碑";
    }

    @Override
    public void init(ModuleContext ctx) {
        registerBlocks();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!on() || !(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
                return;
            }
            placeGrave(player);
        });
    }

    private static synchronized void registerBlocks() {
        if (GRAVE_BLOCK != null) {
            return;
        }
        GRAVE_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                GRAVE_ID,
                new GraveBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                        .mapColor(net.minecraft.world.level.material.MapColor.STONE)
                        .strength(1.5F, 6.0F)
                        .sound(net.minecraft.world.level.block.SoundType.STONE)
                        .noOcclusion() // 非完整方块：邻块接触面不剔除（修复邻块"面透明"）
                        .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, GRAVE_ID))));
        Identifier itemKey = GRAVE_ID; // BlockItem 与方块同 id
        GRAVE_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new net.minecraft.world.item.BlockItem(GRAVE_BLOCK,
                        new net.minecraft.world.item.Item.Properties()
                                .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, itemKey))));
        GRAVE_BE_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                GRAVE_ID,
                new BlockEntityType<>(GraveBlockEntity::new, Set.of(GRAVE_BLOCK)));
        WorldLovesYouMod.LOGGER.info("[世界爱你] 墓碑方块与实体注册完成");
    }

    private static void placeGrave(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        // 取出 die 前暂存的物品+经验
        var snapshot = com.minecraftloveyou.module.graves.GraveHolder.take(player.getUUID());
        if (snapshot == null) {
            return; // 非本模组路径（如 keepInventory 且 mixin 未触发）——不动作
        }
        BlockPos place = null;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = pos.above(dy);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.canBeReplaced()) {
                place = p;
                break;
            }
        }
        if (place == null || !(level instanceof ServerLevel serverLevel)) {
            // 无法放墓碑：把暂存物品全部原地掉落，绝不吞
            WorldLovesYouMod.LOGGER.info("[世界爱你] 玩家 {} 死亡：附近无空位放墓碑，物品原地掉落", snapshot.owner());
            for (var st : snapshot.items()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st);
            }
            if (level instanceof ServerLevel sl) net.minecraft.world.entity.ExperienceOrb.award(sl, player.position(), snapshot.xp());
            return;
        }
        serverLevel.setBlockAndUpdate(place, GRAVE_BLOCK.defaultBlockState());
        if (serverLevel.getBlockEntity(place) instanceof com.minecraftloveyou.module.graves.GraveBlockEntity grave) {
            grave.storeSnapshot(snapshot.items(), snapshot.xp(), snapshot.owner());
            WorldLovesYouMod.LOGGER.info("[世界爱你] 玩家 {} 死亡：墓碑已生成于 {}（{}格物品+{}经验）",
                    snapshot.owner(), place.toShortString(), snapshot.items().size(), snapshot.xp());
        } else {
            WorldLovesYouMod.LOGGER.warn("[世界爱你] 玩家 {} 死亡：墓碑方块实体异常，物品原地掉落", snapshot.owner());
            for (var st : snapshot.items()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st);
            }
            if (level instanceof ServerLevel sl) net.minecraft.world.entity.ExperienceOrb.award(sl, player.position(), snapshot.xp());
        }
    }
}