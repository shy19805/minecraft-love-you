package com.minecraftloveyou.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;

/**
 * 「玩家阵营」判定：玩家本人 + 玩家的宠物。
 *
 * <p>力量射击（powershot）与移除受击无敌帧（rendi）共用同一套作用域口径，
 * 所以放在 core 里，避免两处各写一份、口径漂移。
 *
 * <p>判定规则（按顺序）：
 * <ul>
 *   <li>实体本身就是 {@link Player} → 属于玩家阵营；</li>
 *   <li>实体是 {@link SnowGolem} → 属于玩家阵营（雪傀儡**只能由玩家建造**，没有自然生成，
 *       且原版没有 owner 字段，所以按类型整体归属玩家）；</li>
 *   <li>实体是 {@link OwnableEntity}（驯服的狼/猫/鹦鹉、马/驴/骡/骆驼、驯服的羊驼等），
 *       且 {@code getRootOwner()} 回溯到的最终归属是玩家 → 属于玩家阵营；</li>
 *   <li>其余（怪物、发射器、无主实体、铁傀儡——铁傀儡会自然生成于村庄，不能整体算玩家的）→ 不属于。</li>
 * </ul>
 */
public final class PlayerSide {

    private PlayerSide() {
    }

    /** 该实体是否属于「玩家本人或玩家的宠物」。 */
    public static boolean isPlayerOrPet(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player) {
            return true;
        }
        if (entity instanceof SnowGolem) {
            return true;
        }
        if (entity instanceof OwnableEntity ownable) {
            // getRootOwner 会沿 owner 链回溯（宠物的宠物等），返回最终归属者
            LivingEntity root = ownable.getRootOwner();
            return root instanceof Player;
        }
        return false;
    }
}
