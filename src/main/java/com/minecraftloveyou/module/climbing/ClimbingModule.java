package com.minecraftloveyou.module.climbing;

import com.minecraftloveyou.core.ModuleContext;
import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.core.WlyModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 更好的攀爬 v2：
 * - 速度可调 0~10（默认 2；1=原版；0=完全爬不动）
 * - 只放大纵向攀爬速度，不碰水平分量（修复 v1 左右滑移 bug）
 * - 俯视加速下滑（可关）
 */
public class ClimbingModule implements WlyModule {

    public static final String ID = "climbing";
    /** 攀爬速度倍率（1=原版；0=爬不动；10=10倍）。 */
    public static double climbSpeed = 2.0;
    public static boolean allowDownLookBoost = true;

    public static void register() {
        ModuleManager.register(new ClimbingModule());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "更好的攀爬";
    }

    @Override
    public void init(ModuleContext ctx) {
        // 行为在 mixin
    }

    /** mixin 调用：只调攀爬纵向速度（修 v1 横向滑移 bug）。 */
    public static void applyClimbBoost(Player player) {
        if (!WlyModule.on(ID) || !player.onClimbable() || player.isSpectator()) {
            return;
        }
        Vec3 d = player.getDeltaMovement();
        double speed = Math.max(0.0, Math.min(10.0, climbSpeed));
        boolean moving = player.zza != 0 || player.xxa != 0;
        if (speed <= 0.001) {
            // 完全爬不动：纵向清零
            player.setDeltaMovement(new Vec3(d.x, 0.0, d.z));
            return;
        }
        if (moving) {
            // 上行加速 y>0；下行（按前进键俯视）y<0 同样乘性放大；按潜行（原版下滑）也加速
            if (d.y > 0) {
                d = new Vec3(d.x, d.y * (1.0 + (speed - 1.0) * 0.6), d.z);
            } else if (d.y < 0) {
                d = new Vec3(d.x, d.y * (1.0 + (speed - 1.0) * 0.6), d.z);
            }
        }
        if (allowDownLookBoost && player.getXRot() > 60.0F && !moving) {
            // 纯俯视自由落体式加速下滑
            d = new Vec3(d.x, Math.min(d.y - 0.18 * speed, -0.25 * speed), d.z);
        }
        player.setDeltaMovement(d);
    }
}