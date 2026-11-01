package com.minecraftloveyou.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.minecraft.commands.Commands.literal;

/** /wly 巡检指令：列出模块状态；键位列表由客户端侧另行注册。 */
public class WlyCommands {

    private WlyCommands() {
    }

    /** 消息统一前缀：[模组名]（模组名走语言键，跟随客户端语言）。 */
    public static MutableComponent prefix() {
        return Component.literal("[").append(Component.translatable("wly.modmenu.title")).append(Component.literal("] "));
    }

    /** client=true 时额外注册 /wly keys list（仅客户端可用）。 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean client) {
        var root = literal("wly")
                .then(literal("list").executes(ctx -> listModules(ctx.getSource())))
                .then(literal("fish").executes(ctx -> fishDebug(ctx.getSource())));
        if (client) {
            root = root.then(literal("keys")
                    .then(literal("list").executes(ctx -> com.minecraftloveyou.client.KeyListCommand.list(ctx.getSource()))));
        }
        dispatcher.register(root);
    }

    private static int listModules(CommandSourceStack src) {
        src.sendSuccess(() -> prefix().append(Component.translatable("wly.command.header")), false);
        for (var m : ModuleManager.modules().values()) {
            final boolean on = ModuleManager.enabled(m.id());
            final String id = m.id();
            final String key = m.displayName();
            src.sendSuccess(() -> Component.literal(" - ")
                    .append(Component.translatable(key))
                    .append(Component.literal(" (" + id + "): "))
                    .append(Component.translatable(on ? "wly.command.on" : "wly.command.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        }
        return 1;
    }

    /**
     * 钓鱼取证：把服务端「浮漂为什么不咬钩」判定链上的每一道门一次性打出来。
     *
     * <p>判据来自 javap 26.1.2 的 FishingHook：咬钩计时只在 catchingFish() 里推进，
     * 而 catchingFish() 只在 tick() 判定「浮漂所在方块的流体 is(WATER) 且 getHeight()>0」时才被调用；
     * 计时每次递减 i，i 在「头顶不能见天」时有 50% 概率为 0。
     * 私有字段用反射逐级上溯父类读取（字段可能在版本间搬家，见手册 #14）。
     */
    private static int fishDebug(CommandSourceStack src) {
        if (!(src.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            src.sendSuccess(() -> prefix().append(Component.literal("这条指令需要玩家执行")), false);
            return 0;
        }
        final var hook = player.fishing;
        if (hook == null) {
            src.sendSuccess(() -> prefix().append(Component.literal(
                    "当前没有浮漂（player.fishing = null）——先把钓竿抛进水里再执行本指令")), false);
            return 0;
        }
        final var level = hook.level();
        final var pos = hook.blockPosition();
        final var above = pos.above();
        final var fluid = level.getFluidState(pos);
        final boolean fluidIsWater = fluid.is(net.minecraft.tags.FluidTags.WATER);
        final float height = fluidIsWater ? fluid.getHeight(level, pos) : 0.0F;

        final var main = player.getMainHandItem();
        final var off = player.getOffhandItem();
        final boolean holdingRod = main.is(net.minecraft.world.item.Items.FISHING_ROD)
                || off.is(net.minecraft.world.item.Items.FISHING_ROD);
        final double distSqr = hook.distanceToSqr(player);

        src.sendSuccess(() -> prefix().append(Component.literal("钓鱼取证 ── 按判定链顺序，第一个为 NO 的就是断点"))
                .withStyle(ChatFormatting.AQUA), false);
        report(src, "① 浮漂方块流体是水", fluidIsWater, fluid.getType().toString());
        report(src, "② 流体高度 > 0（决定能否进入 BOBBING/咬钩）", height > 0.0F, "getHeight=" + height);
        report(src, "③ 状态机 currentState", true, String.valueOf(read(hook, "currentState")));
        report(src, "④ 玩家仍持有钓竿（否则钩子被 discard）", holdingRod,
                "main=" + main.getItem() + " off=" + off.getItem());
        report(src, "⑤ 距离 ≤ 32 格", distSqr <= 1024.0D, "dist²=" + Math.round(distSqr));
        report(src, "⑥ 玩家可交互世界", player.canInteractWithLevel(),
                "alive=" + player.isAlive() + " spectator=" + player.isSpectator());
        report(src, "⑦ 头顶可见天（不见天时每刻有 50% 概率不推进计时）", true,
                "canSeeSky=" + level.canSeeSky(above) + " raining=" + level.isRainingAt(above));
        src.sendSuccess(() -> prefix().append(Component.literal(
                        "   计时链 nibble=" + read(hook, "nibble")
                                + "  timeUntilHooked=" + read(hook, "timeUntilHooked")
                                + "  timeUntilLured=" + read(hook, "timeUntilLured")))
                .withStyle(ChatFormatting.GRAY), false);
        src.sendSuccess(() -> prefix().append(Component.literal(
                        "   lureSpeed=" + read(hook, "lureSpeed")
                                + "  luck=" + read(hook, "luck")
                                + "  openWater=" + read(hook, "openWater")
                                + "  outOfWaterTime=" + read(hook, "outOfWaterTime")
                                + "  life=" + read(hook, "life")
                                + "  pos=" + pos.toShortString()))
                .withStyle(ChatFormatting.GRAY), false);
        src.sendSuccess(() -> prefix().append(Component.literal(
                        "   判读：nibble 永远 0 且 ① 全 YES → 咬钩计时没被推进；"
                                + "①或②为 NO → 浮漂没被判定为「在水里」，catchingFish 根本不会被调用")).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static void report(CommandSourceStack src, String label, boolean ok, String detail) {
        final String mark = ok ? "YES" : "NO ";
        src.sendSuccess(() -> prefix().append(Component.literal(
                        label + " : " + mark + "   (" + detail + ")"))
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
    }

    /** 反射读私有字段，沿继承链上溯（字段可能在版本间搬到父类）。 */
    private static Object read(Object target, String fieldName) {
        try {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) {
                    // 继续往父类找
                }
            }
        } catch (Throwable t) {
            return "?" + t.getClass().getSimpleName();
        }
        return "?missing";
    }
}
