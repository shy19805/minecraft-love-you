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
        return "wly.module." + ID;
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

    /**
     * 只做「上限修复」，绝不改血量。
     *
     * <p>原实现有两个问题：
     * ① 用 {@code addPermanentModifier} —— 永久修饰符会被写进玩家存档，脱掉装备后加成仍在（属性残留）；
     * ② {@code 血量 ≈ 20 就把血量补到 20+bonus} —— 每次重进都白送一次满血（可反复利用）。
     *
     * <p>现在：装备加成由原版自己应用，本模块只在检测到「上限确实比装备应给的少」时，
     * 用**瞬时**修饰符补差额（不进存档）；血量只做「不超过新上限」的收口。
     */
    private static void reapply(ServerPlayer p) {
        var attr = p.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        // 先清掉上一轮（或旧版本存档里）留下的本模组修饰符，避免叠加
        attr.removeModifier(MOD);
        double bonus = equipmentHealthBonus(p);
        if (bonus <= 0.001) {
            return;
        }
        double expected = 20.0 + bonus;   // 装备本应提供的上限（基础 20）
        double missing = expected - attr.getValue();
        if (missing <= 0.01) {
            return;                       // 原版已正确应用 → 什么都不做
        }
        attr.addTransientModifier(new AttributeModifier(MOD, missing, AttributeModifier.Operation.ADD_VALUE));
        float max = (float) attr.getValue();
        if (p.getHealth() > max) {
            p.setHealth(max);             // 只收口，不凭空加血
        }
        WorldLovesYouMod.LOGGER.info("[世界爱你] 最大生命修复：{} 上限 {} → {}（装备加成 {}）",
                p.getName().getString(), expected - missing, max, bonus);
    }
}