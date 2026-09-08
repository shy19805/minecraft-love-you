package com.minecraftloveyou.core;

import com.minecraftloveyou.WorldLovesYouMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一键整理（sort）：C2S 整理请求。
 * 规则：按创造物品栏排列方式 = 注册表顺序（命名空间+路径）排序 + 同物合并堆叠。
 * 范围：玩家背包（InventoryMenu 9-44）或打开的箱子/容器（ChestMenu 容器格）。
 */
public final class SortPayload implements CustomPacketPayload {

    public static final SortPayload INSTANCE = new SortPayload();
    public static final Type<SortPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> CODEC =
            StreamCodec.of((buf, p) -> {}, buf -> new SortPayload());

    private SortPayload() {
    }

    @Override
    public Type<? extends SortPayload> type() {
        return TYPE;
    }

    /** 服务端执行整理。 */
    public static void handle(ServerPlayer player) {
        var menu = player.containerMenu;
        if (menu instanceof ChestMenu chest) {
            int n = chest.getContainer().getContainerSize();
            List<Slot> slots = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                slots.add(chest.getSlot(i));
            }
            rearrange(slots);
            chest.getContainer().setChanged();
            chest.broadcastChanges();
        } else if (menu instanceof InventoryMenu inv) {
            // 主背包与快捷栏**分开整理**（快捷栏保持不动）
            List<Slot> main = new ArrayList<>();
            for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_START; i++) {
                main.add(inv.getSlot(i));
            }
            rearrange(main);
            inv.broadcastChanges();
        }
    }

    /**
     * 就地重排：全部用槽内原实例（零 copy，杜绝任何复制 bug）。
     * 1) 收集非空原实例  2) 合并堆叠（grow/shrink 原实例）  3) 排序  4) 槽位重排引用
     */
    private static void rearrange(List<Slot> slots) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Slot slot : slots) {
            stacks.add(slot.getItem());
        }
        // 合并：把后面的堆向前面同物堆合并
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack a = stacks.get(i);
            if (a.isEmpty() || !a.isStackable()) {
                continue;
            }
            for (int j = i + 1; j < stacks.size(); j++) {
                ItemStack b = stacks.get(j);
                if (b.isEmpty() || !ItemStack.isSameItemSameComponents(a, b)) {
                    continue;
                }
                int space = a.getMaxStackSize() - a.getCount();
                if (space > 0) {
                    int move = Math.min(space, b.getCount());
                    a.grow(move);
                    b.shrink(move);
                }
            }
        }
        // 收集非空、排序（注册表顺序）
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) {
                kept.add(s);
            }
        }
        kept.sort(Comparator.comparing(SortPayload::idOf));
        // 放回槽位：前 kept.size() 个放原实例，其余清空
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack cur = slot.getItem();
            ItemStack next = i < kept.size() ? kept.get(i) : ItemStack.EMPTY;
            if (cur != next) {
                if (cur.isEmpty() && next.isEmpty()) {
                    continue;
                }
                slot.set(next);
            }
        }
    }

    private static String idOf(ItemStack s) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        return key.getNamespace() + "." + key.getPath();
    }
}