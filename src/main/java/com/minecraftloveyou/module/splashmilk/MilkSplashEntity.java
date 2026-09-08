package com.minecraftloveyou.module.splashmilk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 喷溅奶瓶实体：命中清除半径内所有实体的全部状态效果（正面+负面）。 */
public class MilkSplashEntity extends ThrowableItemProjectile {

    public MilkSplashEntity(EntityType<? extends MilkSplashEntity> type, Level level) {
        super(type, level);
    }

    public MilkSplashEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(SplashMilkModule.MILK_ENTITY, owner, level, stack);
    }

    public MilkSplashEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(SplashMilkModule.MILK_ENTITY, x, y, z, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return SplashMilkModule.SPLASH_MILK_ITEM;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel serverLevel) {
            double radius = SplashMilkModule.splashRange; // 3 格生效半径
            AABB box = this.getBoundingBox().inflate(radius, 0.5, radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box);
            for (LivingEntity t : targets) {
                t.removeAllEffects();
            }
            // 喷溅型药水同款：白色效果粒子按半径扩散 + 水花
            var white = net.minecraft.core.particles.ColorParticleOption.create(
                    net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0.98F, 0.98F, 0.95F);
            serverLevel.sendParticles(white, getX(), getY() + 0.1, getZ(), (int) (40 * radius), radius * 0.6, 0.4, radius * 0.6, 0.02);
            serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.2, getZ(), (int) (30 * radius), radius * 0.5, 0.3, radius * 0.5, 0.1);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        // 命中实体额外清一次（保险）
        if (result.getEntity() instanceof LivingEntity le && !level().isClientSide()) {
            le.removeAllEffects();
        }
    }
}