package com.minecraftloveyou.module.portablespawner;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 便携刷怪笼（portable-spawner）：
 * ① 潜行+手持便携刷怪笼右键刷怪笼 → 吸取类型（原笼变空猪笼），类型存入物品 CustomData
 * ② 手持已吸类型的便携刷怪笼右键地面 → 放置刷怪笼并写入类型
 */
public class PortableSpawnerModule implements WlyModule {

    public static final String ID = "portable-spawner";
    public static Item PORTABLE_SPAWNER_ITEM;

    public static void register() {
        registerItem();
        ModuleManager.register(new PortableSpawnerModule());
    }

    private static synchronized void registerItem() {
        if (PORTABLE_SPAWNER_ITEM != null) {
            return;
        }
        Identifier id = Identifier.fromNamespaceAndPath("minecraft_love_you", "portable_spawner");
        PORTABLE_SPAWNER_ITEM = net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.ITEM, id,
                new Item(new Item.Properties().stacksTo(1)
                        .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id))));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
                .modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(PORTABLE_SPAWNER_ITEM));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "便携刷怪笼";
    }

    @Override
    public void init(ModuleContext ctx) {
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!on() || world.isClientSide() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(PORTABLE_SPAWNER_ITEM)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();

        // 吸取
        if (world.getBlockEntity(pos) instanceof SpawnerBlockEntity spawnerBE && player.isSecondaryUseActive()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) world;
            CompoundTag beTag = new CompoundTag();
            spawnerBE.saveCustomOnly(serverLevel.registryAccess(), beTag);
            CompoundTag spawnData = beTag.getCompound("SpawnData").orElseGet(CompoundTag::new);
            CompoundTag entity = spawnData.getCompound("entity").orElseGet(CompoundTag::new);
            String eid = entity.getString("id").orElse("");
            if (eid.isEmpty()) {
                return InteractionResult.PASS;
            }
            held.set(DataComponents.CUSTOM_DATA, CustomData.of(
                    new CompoundTag() {{ putString("EntityType", eid); }}));
            // 原笼清空为猪
            spawnerBE.getSpawner().setEntityId(EntityType.PIG, world, player.getRandom(), pos);
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "[我的世界爱你] 已吸取：" + eid), true);
            return InteractionResult.SUCCESS;
        }

        // 放置
        var data = held.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("EntityType")) {
            String eid = data.copyTag().getString("EntityType").orElse("");
            if (eid.isEmpty()) {
                return InteractionResult.PASS;
            }
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getValue(Identifier.parse(eid));
            if (type == null) {
                return InteractionResult.PASS;
            }
            BlockPos placePos = pos.relative(hit.getDirection());
            var below = world.getBlockState(placePos.below());
            if (!world.getBlockState(placePos).isAir() || !below.isFaceSturdy(world, placePos.below(), net.minecraft.core.Direction.UP)) {
                return InteractionResult.PASS;
            }
            world.setBlockAndUpdate(placePos, Blocks.SPAWNER.defaultBlockState());
            if (world.getBlockEntity(placePos) instanceof SpawnerBlockEntity newBE) {
                newBE.getSpawner().setEntityId(type, world, player.getRandom(), placePos);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
