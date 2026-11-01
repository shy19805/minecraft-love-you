package com.minecraftloveyou.module.mendhold;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * 更好的经验修补：Shift+右键，直接用自身经验修复（带"经验修补"的物品）。
 * 修复顺序：主手 → 副手 → 装备（破损优先）。
 */
public class MendHoldModule implements WlyModule {

    public static final String ID = "mend-hold";
    /** 每修 1 耐久消耗的经验点（默认 2）。 */
    public static int costPerDurability = 2;

    public static void register() {
        ModuleManager.register(new MendHoldModule());
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
    public void init(ModuleContext ctx) {
        UseItemCallback.EVENT.register(this::onUseItem);
    }

    private InteractionResult onUseItem(Player player, Level world, InteractionHand hand) {
        if (!on() || world.isClientSide() || player.isSpectator() || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        Holder<Enchantment> mending;
        try {
            mending = world.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.MENDING);
        } catch (Exception e) {
            return InteractionResult.PASS;
        }
        int xp = sp.totalExperience;
        if (xp <= 0) {
            return InteractionResult.PASS;
        }
        boolean mended = false;
        for (ItemStack stack : orderedMendables(sp)) {
            if (!isMendable(stack, mending)) {
                continue;
            }
            int dmg = stack.getDamageValue();
            if (dmg <= 0) {
                continue;
            }
            // 关键：每轮都重新读取 totalExperience。早期用循环外的快照 xp 计算 repair，
            // 修完第一件后 xp 已减少但快照没变 → 第二件按「原本的经验」继续修，
            // 总经验被扣成负数 → recalcLevels 夹到 0 → 玩家白嫖耐久。
            int xpNow = Math.max(0, sp.totalExperience);
            int repair = Math.min(dmg, xpNow / Math.max(1, costPerDurability));
            if (repair <= 0) {
                break;
            }
            stack.setDamageValue(dmg - repair);
            sp.totalExperience = xpNow - repair * costPerDurability;
            recalcLevels(sp);
            mended = true;
            world.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.6F);
            if (sp.totalExperience <= 0) {
                break;
            }
        }
        return mended ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static java.util.List<ItemStack> orderedMendables(ServerPlayer sp) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        list.add(sp.getMainHandItem());
        list.add(sp.getOffhandItem());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            list.add(sp.getItemBySlot(slot));
        }
        return list;
    }

    private static boolean isMendable(ItemStack stack, Holder<Enchantment> mending) {
        if (stack.isEmpty() || !stack.has(DataComponents.MAX_DAMAGE) || stack.getDamageValue() <= 0) {
            return false;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(mending, stack) > 0;
    }

    /** 按经验总量重算等级（原版三段曲线）。 */
    private static void recalcLevels(ServerPlayer sp) {
        int total = Math.max(0, sp.totalExperience);
        int level = 0;
        int remain = total;
        while (true) {
            int need = xpToNext(level);
            if (remain < need) {
                break;
            }
            remain -= need;
            level++;
        }
        sp.setExperienceLevels(level);
        sp.setExperiencePoints(remain);
    }

    private static int xpToNext(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 2 * level + 7;
    }
}