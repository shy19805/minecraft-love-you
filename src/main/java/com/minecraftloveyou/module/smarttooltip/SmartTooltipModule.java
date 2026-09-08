package com.minecraftloveyou.module.smarttooltip;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 智能提示（smart-tooltip）：物品 tooltip 追加实用信息——
 * 耐久剩余（可损耗物品）、堆叠上限、物品 id（F3+H 风格）。
 */
public class SmartTooltipModule implements WlyModule {

    public static final String ID = "smart-tooltip";

    public static void register() {
        ModuleManager.register(new SmartTooltipModule());
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
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void init(ModuleContext ctx) {
        // 注册在 client：registerClient
    }

    public static void registerClient() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!WlyModule.on(ID) || stack.isEmpty() || lines == null) {
                return;
            }
            // 耐久剩余
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                int left = stack.getMaxDamage() - stack.getDamageValue();
                float pct = (float) left / stack.getMaxDamage();
                net.minecraft.ChatFormatting color = pct > 0.5f ? net.minecraft.ChatFormatting.GREEN
                        : (pct > 0.25f ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.RED);
                lines.add(Component.translatable("wly.ui.smart.durability", left, stack.getMaxDamage(), Math.round(pct * 100))
                        .withStyle(color));
            }
            // 堆叠上限（>1 时）
            int max = stack.getMaxStackSize();
            if (max > 1) {
                lines.add(Component.translatable("wly.ui.smart.stack", stack.getCount(), max)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 物品 id（高级 tooltip 常显）
            if (tooltipType.isAdvanced()) {
                var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                lines.add(Component.translatable("wly.ui.smart.item_id", key)
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        });
    }
}