package com.example.hardai;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class MyAttackableTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal {
    private final MyTargetingConditions myTargetConditions;

    public MyAttackableTargetGoal(Mob attacker, Class<T> targetType, boolean mustSee) {
        this(attacker, targetType, 10,  mustSee, false, (Predicate<LivingEntity>)null);
    }

    public MyAttackableTargetGoal(Mob attacker, Class<T> targetType, int p_26055_, boolean mustSee, boolean mustReachToTrack, @Nullable Predicate<LivingEntity> p_26058_) {
        super(attacker, targetType, p_26055_, mustSee, mustReachToTrack, p_26058_);
        this.myTargetConditions = MyTargetingConditions.forCombat().range(this.getFollowDistance()).selector(p_26058_);
        this.unseenMemoryTicks = 400; // make high
    }

    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        } else {
            this.findTarget();
            return this.target != null;
        }
    }

    protected AABB getTargetSearchArea(double p_26069_) {
        return this.mob.getBoundingBox().inflate(p_26069_, 4.0D, p_26069_);
    }

    protected void findTarget() {
        if (this.targetType != Player.class && this.targetType != ServerPlayer.class) {
            this.target = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(
                    this.targetType,
                            this.getTargetSearchArea(this.getFollowDistance()), (p_148152_) -> true),
                    this.myTargetConditions.createVanillaTargetingConditions(this.mob),
                    this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ()
            );

        } else {
            this.target = this.mob.level.getNearestPlayer(
                    this.myTargetConditions.createVanillaTargetingConditions(this.mob),
                    this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ()
            );
        }

    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    public void setTarget(@Nullable LivingEntity p_26071_) {
        this.target = p_26071_;
    }
}
