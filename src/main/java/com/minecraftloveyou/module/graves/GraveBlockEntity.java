package com.minecraftloveyou.module.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 墓碑方块实体：保存死亡玩家的全部物品与经验；右键整体转交。 */
public class GraveBlockEntity extends BaseContainerBlockEntity {

    public static final int SIZE = 54;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int storedXp = 0;
    private String ownerName = "";

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(GravesModule.GRAVE_BE_TYPE, pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items.clear();
        this.items.addAll(items);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("墓碑");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6, syncId, inventory, this, 6);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, false);
        output.putInt("StoredXp", storedXp);
        output.putString("Owner", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        storedXp = input.getIntOr("StoredXp", 0);
        ownerName = input.getStringOr("Owner", "");
    }

    /** 从死亡暂存写入（die 摘取的物品+经验）。 */
    public void storeSnapshot(List<ItemStack> stacks, int xp, String owner) {
        int idx = 0;
        for (ItemStack stack : stacks) {
            if (idx >= SIZE) {
                // 墓碑装不下：掉在墓碑脚下
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, stack);
                continue;
            }
            items.set(idx, stack);
            idx++;
        }
        this.storedXp = xp;
        this.ownerName = owner;
        setChanged();
    }

    /** 死亡时填充内容（物品+经验）：遍历玩家全部背包/装备槽。 */
    public void storeFrom(ServerPlayer player) {
        var inv = player.getInventory();
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                all.add(stack.copy());
            }
        }
        int idx = 0;
        for (ItemStack stack : all) {
            if (idx >= SIZE) {
                // 装不下：原地掉落，绝不吞物品
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                continue;
            }
            items.set(idx, stack);
            idx++;
        }
        storedXp = player.totalExperience;
        ownerName = player.getName().getString();
        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        setChanged();
    }

    /** 右键取回：给玩家物品与经验。 */
    public void giveAllTo(ServerPlayer player) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                }
                items.set(i, ItemStack.EMPTY);
            }
        }
        if (storedXp > 0) {
            ExperienceOrb.award((ServerLevel) player.level(), player.position(), storedXp);
            storedXp = 0;
        }
        setChanged();
    }

    /** 墓碑被破坏/过期：物品与经验全部掉出。 */
    public void dropAll(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
        if (storedXp > 0 && level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), storedXp);
            storedXp = 0;
        }
    }
}