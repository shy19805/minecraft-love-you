package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.client.EnchantRevealCache;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * enchant-reveal 客户端：悬停附魔台档位行时显示"全部候选"。
 * 挂在父类 AbstractContainerScreen（@Shadow 不能跨父类取字段），
 * instance-of 保证只在附魔台界面生效。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class EnchantmentScreenMixin {

    @Shadow
    protected net.minecraft.world.inventory.Slot hoveredSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void wly$revealTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!WlyModule.on("enchant-reveal")) {
            return;
        }
        if (!((Object) this instanceof EnchantmentScreen self)) {
            return;
        }
        EnchantmentMenu menu = self.getMenu();
        var registry = net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        for (int i = 0; i < 3; i++) {
            if (menu.enchantClue[i] < 0 || menu.costs[i] <= 0) {
                continue;
            }
            // 悬停判定与原版 clue 区域一致（60,14+19i 108x17）
            boolean over = mouseX >= leftPos + 60 && mouseX < leftPos + 60 + 108
                    && mouseY >= topPos + 14 + 19 * i && mouseY < topPos + 14 + 19 * i + 17;
            if (!over) {
                continue;
            }
            var texts = new java.util.ArrayList<Component>();
            var clueEnchant = registry.get(menu.enchantClue[i]);
            if (clueEnchant.isPresent()) {
                texts.add(Component.translatable("container.enchant.clue",
                        net.minecraft.world.item.enchantment.Enchantment.getFullname(clueEnchant.get(), menu.levelClue[i]))
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
            }
            List<String> offers = EnchantRevealCache.offers().get(i);
            if (offers != null && !offers.isEmpty()) {
                texts.add(Component.empty());
                texts.add(Component.literal("§e可能附魔（全部候选）："));
                for (String s : offers) {
                    texts.add(Component.literal("§7· " + s));
                }
            }
            graphics.setComponentTooltipForNextFrame(self.getFont(), texts, mouseX, mouseY);
            return;
        }
    }
}