package com.minecraftloveyou.mixin.client;

import com.minecraftloveyou.core.WlyModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 全键无冲（nonconflict-keys）：
 * 按键设置里的"黄色 [ ] 冲突警告"（duplicateKeybinds）不再显示——
 * 同键绑定的多个功能在 26.2 本就全部激活（KeyMapping.click/set 遍历所有同键映射），
 * 这里只消掉视觉冲突标记。
 *
 * <p><b>目标必须是 KeyEntry 而不是 Entry</b>：`KeyBindsList$Entry.refreshEntry()` 是
 * **abstract**（无字节码），@Redirect 挂上去必然不生效；真正实现（含
 * `KeyMapping.same(...)` 调用）在子类 `KeyBindsList$KeyEntry.refreshEntry()` 里。
 * 26.1.2 / 26.2 两版一致。
 */
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindsListMixin {

    @Redirect(
            method = "refreshEntry",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;same(Lnet/minecraft/client/KeyMapping;)Z"))
    private boolean wly$noConflictMark(KeyMapping other, KeyMapping mine) {
        if (WlyModule.on("nonconflict-keys")) {
            return false; // 视为不冲突
        }
        return other.same(mine);
    }
}