package com.example.hardai;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class MyTargetingConditions { // reimplement entire class without subtyping because TargetingConditions's constructor is private (very cool).

   public static final MyTargetingConditions DEFAULT = forCombat();

   private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0D;
   private final boolean isCombat;
   private double range = -1.0D;
   private boolean checkLineOfSight = true;

   private boolean mayRelyOnSightOnly = true;

   private boolean testInvisible = true;
   @Nullable
   private Predicate<LivingEntity> selector;

   public MyTargetingConditions() {
      isCombat = false;
   }

   public MyTargetingConditions(boolean isCombat) {
      this.isCombat = isCombat;
   }

   public static MyTargetingConditions forCombat() {
      return new MyTargetingConditions(true);
   }

   public static MyTargetingConditions forNonCombat() {
      return new MyTargetingConditions(false);
   }

   public MyTargetingConditions range(double p_26884_) {
      this.range = p_26884_;
      return this;
   }

   public MyTargetingConditions ignoreLineOfSight() {
      this.checkLineOfSight = false;
      return this;
   }

   public MyTargetingConditions ignoreInvisibilityTesting() {
      this.testInvisible = false;
      return this;
   }

   public MyTargetingConditions selector(@Nullable Predicate<LivingEntity> p_26889_) {
      this.selector = p_26889_;
      return this;
   }

   public TargetingConditions createVanillaTargetingConditions(LivingEntity me) {
      //turn off all vanilla checks and delegate to this.test
      return TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting().range(-1).selector(target -> this.test(me, target));
   }

   public boolean test(@Nullable LivingEntity me, LivingEntity target) {
      if (me == target) {
         return false;
      } else if (!target.canBeSeenByAnyone()) {
         return false;
      } else if (this.selector != null && !this.selector.test(target)) {
         return false;
      } else {
         if (me == null) {
            if (this.isCombat && (!target.canBeSeenAsEnemy() || target.level.getDifficulty() == Difficulty.PEACEFUL)) {
               return false;
            }
         } else {
            if (this.isCombat && (!me.canAttack(target) || !me.canAttackType(target.getType()) || me.isAlliedTo(target))) {
               return false;
            }

            double visibilityPercent = this.testInvisible ? target.getVisibilityPercent(me) : 1.0D;
            double visibility = visibilityPercent > 0 ? 1 : 0; //sorry folks, not invisible is visible.
            boolean targetIsVisible = visibility > 0;
            double visibilityRange = Math.max(this.range * visibility, MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET);

            boolean canSeeTarget = this.mayRelyOnSightOnly && targetIsVisible && me instanceof Mob && ((Mob)me).getSensing().hasLineOfSight(target);
            if (canSeeTarget) {
               return true;
            }

            if (this.range > 0.0D) {
               double d2 = me.distanceToSqr(target.getX(), target.getY(), target.getZ());
               if (d2 > visibilityRange * visibilityRange) {
                     return false;
               }
            }

            if (this.checkLineOfSight && me instanceof Mob) {
               Mob mob = (Mob)me;
               if (!mob.getSensing().hasLineOfSight(target)) {
                  return false;
               }
            }
         }

         return true;
      }
   }
}
