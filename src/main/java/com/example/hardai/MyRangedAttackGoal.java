package com.example.hardai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;

public class MyRangedAttackGoal<T extends net.minecraft.world.entity.Mob & RangedAttackMob> extends Goal {
    private final T mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackCooldown = -1;
    private int attentionTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public <M extends Monster & RangedAttackMob> MyRangedAttackGoal(M p_25792_, double p_25793_, int p_25794_, float p_25795_){
        this((T) p_25792_, p_25793_, p_25794_, p_25795_);
    }

    public MyRangedAttackGoal(T p_25792_, double p_25793_, int p_25794_, float attackRadius) {
        this.mob = p_25792_;
        this.speedModifier = p_25793_;
        this.attackIntervalMin = p_25794_;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void setMinAttackInterval(int p_25798_) {
        this.attackIntervalMin = p_25798_;
    }

    public boolean canUse() {
        return this.mob.getTarget() == null ? false : this.isHoldingBow();
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(is -> is.getItem() instanceof BowItem);
    }

    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.attentionTime = 0;
        this.attackCooldown = -1;
        this.mob.stopUsingItem();
        ExampleMod.LOGGER.info("stopping tracking");
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            boolean seesTarget = this.mob.getSensing().hasLineOfSight(livingentity);
            boolean attentionOnTarget = this.attentionTime > 0;
            if (seesTarget != attentionOnTarget) {
                this.attentionTime = 0;
            }

            if (seesTarget) {
                ++this.attentionTime;
            } else {
                --this.attentionTime;
            }

            double sqrDistanceToTarget = this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean inRange = sqrDistanceToTarget < (double) this.attackRadiusSqr;
            if (inRange && this.attentionTime >= 20) {
                this.mob.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.mob.getNavigation().moveTo(livingentity, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (this.mob.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if (this.mob.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                boolean nearTarget = sqrDistanceToTarget < (double)(this.attackRadiusSqr * 0.75F);
                if (nearTarget) {
                    this.strafingBackwards = true;
                } else {
                    this.strafingBackwards = false;
                }

                this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.mob.lookAt(livingentity, 30.0F, 30.0F);
            } else {
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            }

            boolean hasLostAttention = this.attentionTime < -60;
            hasLostAttention = false;
            attackCooldown--;
            if (this.mob.isUsingItem()) {
                if (!seesTarget && hasLostAttention) {
                    this.mob.stopUsingItem(); //skelly releases charged bow cuz it's not paying attention
                } else if (seesTarget) {
                    int i = this.mob.getTicksUsingItem();
                    if (i >= 20) {
                        this.mob.stopUsingItem(); // skelly shoots
                        this.mob.performRangedAttack(livingentity, BowItem.getPowerForTime(i));
                        this.attackCooldown = this.attackIntervalMin;
                    }
                }
            } else if (attackCooldown <= 0 && !hasLostAttention) {
                this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof BowItem));
            }

        }
    }
}
