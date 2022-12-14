package com.example.hardai;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;

public class MyRangedAttackGoal extends Goal {
    private final MySkeleton mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackCooldown = -1;
    private int attentionTime;
    private boolean strafingClockwise;
    private int strafingTime = -1;

    final static private int TICKS_TILL_BOW_CHARGED = 20;

    private enum StrafeDirection {FORWARD, BACKWARD, RAND_STRAFE_BACK, RAND_STRAFE_FORWARD;
        public boolean isLeisureStrafe() {
            return this == RAND_STRAFE_FORWARD || this == RAND_STRAFE_BACK;
        }

        public boolean isBackward() {
            return this == BACKWARD || this == RAND_STRAFE_BACK;
        }

        public StrafeDirection invertLeisureStrafe() {
            if (this == StrafeDirection.RAND_STRAFE_FORWARD) {
                return StrafeDirection.RAND_STRAFE_BACK;
            } else if (this == StrafeDirection.RAND_STRAFE_BACK) {
                return StrafeDirection.RAND_STRAFE_FORWARD;
            }
            return this;
        }
    }
    private StrafeDirection strafeDirection = StrafeDirection.RAND_STRAFE_FORWARD;

    public MyRangedAttackGoal(MySkeleton p_25792_, double p_25793_, int p_25794_, float attackRadius) {
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
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            boolean seesTarget = this.mob.getSensing().hasLineOfSight(target);
            boolean attentionOnTarget = this.attentionTime > 0;
            if (seesTarget != attentionOnTarget) {
                this.attentionTime = 0;
            }

            if (seesTarget) {
                ++this.attentionTime;
            } else {
                --this.attentionTime;
            }

            boolean shotAtTarget = false;
            if (this.mob.isUsingItem() && mob.distanceToSqr(target.position()) < attackRadiusSqr * 0.8) {
                int i = this.mob.getTicksUsingItem();
                if (i >= TICKS_TILL_BOW_CHARGED && mob.isTargetFreeFromKnockback()) {
                    ExampleMod.LOGGER.info("in range, finding shot");
                    var possibleShot = mob.tryToFindShot(target);
                    shotAtTarget = possibleShot.isPresent();
                    possibleShot.ifPresent(shot -> {
                        this.mob.stopUsingItem(); // skelly shoots
                        this.mob.performRangedAttack(BowItem.getPowerForTime(i), shot);
                        ExampleMod.LOGGER.info("in range and shoot");
                        this.attackCooldown = this.attackIntervalMin;
                    });
                }
            } else if (attackCooldown <= 0) {
                this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof BowItem));
            }

            double sqrDistanceToTarget = this.mob.distanceToSqr(target.position());
            boolean inRange = sqrDistanceToTarget < (double) this.attackRadiusSqr;
            if (shotAtTarget || (inRange && this.attentionTime >= 20)) {
                this.mob.getNavigation().stop();
                ++this.strafingTime;
            } else if (!shotAtTarget) {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (this.mob.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if (this.mob.getRandom().nextFloat() < 0.3) {
                    strafeDirection = strafeDirection.invertLeisureStrafe();
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                boolean nearTarget = sqrDistanceToTarget < (double)(this.attackRadiusSqr * 0.75F);
                boolean tryToEscape = sqrDistanceToTarget < (double)(this.attackRadiusSqr * 0.5F);
                if (nearTarget && !tryToEscape) {
                    boolean moveForward = mob.getRandom().nextFloat() < 0.5;
                    strafeDirection = moveForward ? StrafeDirection.RAND_STRAFE_FORWARD : StrafeDirection.RAND_STRAFE_BACK;
                } else if (tryToEscape){
                    strafeDirection = StrafeDirection.BACKWARD;
                } else { // far away
                    strafeDirection = StrafeDirection.FORWARD;
                }

                float speed = strafeDirection.isLeisureStrafe() ? 0.5f : 1;
                float direction = strafeDirection.isBackward() ? -1 : 1;
                float clockwiseStrafeSpeed = this.strafingClockwise ? 0.5F : -0.5F;
                if (tryToEscape) {
                    clockwiseStrafeSpeed = 0;
                }
                //this.mob.getMoveControl().strafe(speed * direction, clockwiseStrafeSpeed);
                this.mob.getMoveControl().strafe(speed * direction, 0);
                this.mob.lookAt(target, 30.0F, 30.0F);
            } else {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            attackCooldown--;

        }
    }
}
