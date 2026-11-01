package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 去实验设置警告（进入已有世界一侧）：把「自定义世界 / 实验性世界」的劝告屏全部拿掉。
 *
 * <p>三道闸，对应 26.2 {@code WorldOpenFlows} 上的三条真实路径（26.1.2 字节码一致）：
 * <ol>
 *   <li><b>{@code askForBackup}</b> —— 真正的劝告屏。打开旧「自定义世界」或实验性世界时，
 *       {@code openWorldCheckWorldStemCompatibility} 会调它弹 {@code BackupConfirmScreen}
 *       （selectWorld.backupQuestion.customized / .experimental）。
 *       在 HEAD 直接执行第 3 个参数（继续回调）并 cancel，等价于点「我知道我在做什么」。</li>
 *   <li><b>{@code openWorldCheckWorldStemCompatibility}</b> —— 劝告的判定源。
 *       参考模组 DisableCustomWorldsAdvice 的做法：把
 *       {@code WorldData.worldGenSettingsLifecycle()} 的重定向结果钉成 {@code Lifecycle.stable()}，
 *       让 {@code isExperimental} 判定恒为 false，从源头掐掉实验性分支。</li>
 *   <li><b>{@code confirmWorldCreation}</b> —— 创建/确认世界时的实验性 / 已弃用警告。
 *       参考同一模组的做法：第 5 个参数（boolean，语义是「已确认，直接继续」）强制为 true。
 *       原版字节码：{@code if (bl || lifecycle == stable) onProceed.run(); else 弹 ConfirmScreen}。</li>
 * </ol>
 *
 * <p><b>参数核对（javap -c）</b>：
 * {@code askForBackup(access, customized, onProceed, onCancel)}——slot 3 被
 * {@code BackupConfirmScreen} 的 proceed listener 捕获（{@code lambda$askForBackup$0}），
 * slot 4 才是取消回调，故第 3 参即「继续」；
 * {@code confirmWorldCreation(mc, screen, lifecycle, onProceed, bl)} 是静态方法，bl 落在 slot 4。
 */
@Mixin(WorldOpenFlows.class)
public abstract class NoExpWarnLoadMixin {

    @Inject(method = "askForBackup", at = @At("HEAD"), cancellable = true)
    private void wly$skipBackupAsk(LevelStorageSource.LevelStorageAccess access, boolean customized,
                                   Runnable onProceed, Runnable onCancel, CallbackInfo ci) {
        if (WlyModule.on("no-exp-warn")) {
            onProceed.run(); // 等同于玩家点了「继续」，不弹备份询问
            ci.cancel();
        }
    }

    @Redirect(method = "openWorldCheckWorldStemCompatibility",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle wly$stableWorldLifecycle(WorldData data) {
        return WlyModule.on("no-exp-warn") ? Lifecycle.stable() : data.worldGenSettingsLifecycle();
    }

    @ModifyVariable(method = "confirmWorldCreation", at = @At("HEAD"), argsOnly = true, index = 4, require = 1)
    private static boolean wly$alwaysProceed(boolean alreadyConfirmed) {
        return WlyModule.on("no-exp-warn") || alreadyConfirmed;
    }
}
