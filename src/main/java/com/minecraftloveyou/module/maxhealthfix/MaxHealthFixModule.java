package com.minecraftloveyou.module.maxhealthfix;

import com.minecraftloveyou.WorldLovesYouMod;
import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * 最大生命修复：进入存档时重新应用装备提供的生命上限加成，
 * 生命值按同比例补回，避免"重进后上限塌回 20"（原 MC-17876 类问题）。
 * 挂为道具类修复，细节待真机确认。
 */
public class MaxHealthFixModule implements WlyModule {

    public static final String ID = "maxhealth-fix";
    private static final Identifier MOD = Identifier.fromNamespaceAndPath(WorldLovesYouMod.MOD_ID, "maxhealth_fix");

    public static void register() {
        ModuleManager.register(new MaxHealthFixModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "最大生命修复";
    }

    @Override
    public void init(ModuleContext ctx) {
        if (!on()) {
            return;
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!on()) {
                return;
            }
            ServerPlayer p = handler.player;
            server.execute(() -> reapply(p));
        });
    }

    /** 汇总身上所有装备槽位的 MAX_HEALTH 修饰（按原版三运算合成）。 */
    private static double equipmentHealthBonus(ServerPlayer p) {
        double add = 0;
        double mulBase = 1;
        double mulTotal = 1;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = p.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final double[] slotAdd = {0};
            final double[] slotMulBase = {1};
            final double[] slotMulTotal = {1};
            stack.forEachModifier(slot, (attr, mod) -> {
                if (attr.value() == Attributes.MAX_HEALTH.value()) {
                    switch (mod.operation()) {
                        case ADD_VALUE -> slotAdd[0] += mod.amount();
                        case ADD_MULTIPLIED_BASE -> slotMulBase[0] *= 1 + mod.amount();
                        case ADD_MULTIPLIED_TOTAL -> slotMulTotal[0] *= 1 + mod.amount();
                    }
                }
            });
            add += slotAdd[0];
            mulBase *= slotMulBase[0];
            mulTotal *= slotMulTotal[0];
        }
        return (20.0 * (mulBase - 1)) * mulTotal + add * mulTotal;
    }

    private static void reapply(ServerPlayer p) {
        double bonus = equipmentHealthBonus(p);
        var attr = p.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        attr.removeModifier(MOD);
        if (bonus > 0.001) {
            attr.addPermanentModifier(new AttributeModifier(MOD, bonus, AttributeModifier.Operation.ADD_VALUE));
            // 若生命被加载过程塌回 20，按加成补回（比例 100% 的保守策略） // 待与用户确认
            if (Math.abs(p.getHealth() - 20.0F) < 0.01F && bonus > 0) {
                p.setHealth((float) Math.min(20.0 + bonus, attr.getValue()));
            }
        }
    }
}