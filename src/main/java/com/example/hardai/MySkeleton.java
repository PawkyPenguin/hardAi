package com.example.hardai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;

import java.util.ArrayList;
import java.util.Optional;

public class MySkeleton extends Skeleton {
    private static final double FOLLOW_RANGE = 32;
    private static final double ATTACK_RADIUS = 45;
    private final MyRangedAttackGoal bowGoal = makeBowGoal();
    private final MeleeAttackGoal meleeGoal = makeMeleeGoal();
    private static final Vec3 ARROW_GRAVITY = new Vec3(0, -0.05, 0);
    private static final float ARROW_BOUNDING_BOX_RADIUS = 0.25f;
    private AbstractArrow lastArrow;

    public final static float ARROW_SPEED = 1.6f;
    private MyRangedAttackGoal makeBowGoal() {return new MyRangedAttackGoal(this, 1.0D, 20, (float) ATTACK_RADIUS);}

    public class Shot {
        public double t;
        public Vec3 v;
        public Shot(double t, Vec3 v) {
            this.t = t;
            this.v = v;
        }

        public String toString() {
            return "Shot[t = " + t + ", v = " + v + "]";
        }
    }

    private MeleeAttackGoal makeMeleeGoal() {
        return new MeleeAttackGoal(this, 1.2D, false) {
            public void stop() {
                super.stop();
                MySkeleton.this.setAggressive(false);
            }

            public void start() {
                super.start();
                MySkeleton.this.setAggressive(true);
            }
        };
    }

    public MySkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
        reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    public Optional<Shot> tryToFindShot(LivingEntity target) {
        final float eyeHeightOffsetForArrowSpawn = -0.1f; // magic value, from AbstractArrow constructor
        Vec3 arrowStartPos = new Vec3(getX(), getEyeY() + eyeHeightOffsetForArrowSpawn, getZ());
        Shot[] shots = this.calculateShotsSortedByImpactTime(target, arrowStartPos);
        for (var shot : shots) {
            if (wouldArrowHit(arrowStartPos, shot)) {
                return Optional.of(shot);
            }
        }
        return Optional.empty();
    }

    //TODO don't take center. Choose proper method.
    private Vec3 getTargetPoint() {
        return getTarget().getEyePosition();
    }

    private boolean doesArrowHitTarget(VoxelShape voxelshape, Vec3 p, Vec3 lastPos, BlockPos blockpos) {
        if (!voxelshape.isEmpty()) {
            Vec3 arrowToTarget = getTargetPoint().subtract(lastPos);
            Vec3 arrowDirection = p.subtract(lastPos);
            double distanceToTarget = arrowDirection.dot(arrowToTarget);
            for (AABB aabb : voxelshape.toAabbs()) {
                AABB obstacleBoundingBox = aabb.move(blockpos);
                if (obstacleBoundingBox.contains(p)) {
                    Vec3 arrowToObstacle = obstacleBoundingBox.getCenter().subtract(lastPos);
                    // I *think* this is sound. The center of a bounding box is given by the center of its three axes.
                    // The axes are independent. Therefore, the projection of the center onto the arrow "ray" is also independent per axis.
                    // If the target's center is in front of the obstacle's center (viewing from the arrow), we collide with the target first.
                    double distanceToObstacle = arrowDirection.dot(arrowToObstacle);
                    boolean weHitTarget = distanceToTarget < distanceToObstacle;
                    return weHitTarget;
                }
            }
        }
        return true;
    }

    private boolean doesArrowCollideWithBlock(VoxelShape voxelshape, Vec3 p, BlockPos blockPos) {
        if (false && !voxelshape.isEmpty()) { //TODO
            for (AABB aabb : voxelshape.toAabbs()) {
                if (aabb.move(blockPos).contains(p)) {
                    ExampleMod.LOGGER.info("gonna shoot a block within first ticks");
                    return true;
                }
            }
        }
        return false;

    }

    public boolean isTargetFreeFromKnockback() {
        if (!(getTarget() instanceof Player)) {
            return true;
        } else {
            return getTarget().tickCount - getTarget().getLastHurtByMobTimestamp() > 20 || getTarget().tickCount - CommonMCEvents.sinceOnGroundTimestamp > 8;
        }
    }

    private boolean wouldArrowHit(Vec3 arrowPos, Shot shot) {
        Vec3 p = arrowPos;
        Vec3 lastPos = p;
        Vec3 v = shot.v;
        for (int t = 0; t < Math.floor(shot.t); t++) {
            lastPos = p;
            p = p.add(v);
            BlockPos blockpos = new BlockPos(p);
            BlockState blockstate = this.level.getBlockState(blockpos);
            //if (!blockstate.isAir()) {
            //    VoxelShape voxelshape = blockstate.getCollisionShape(this.level, blockpos);
            //    if (doesArrowCollideWithBlock(voxelshape, p, blockpos)) {
            //        return false;
            //    }
            //}
            if (doesAnyCornerHitWall(p, lastPos, getTargetPoint(), ARROW_BOUNDING_BOX_RADIUS)) {
                return false;
            }
            //ExampleMod.LOGGER.info("Simulating position: " + p);

            double friction = 0.99;
            if (blockstate.getFluidState().getFluidType() == ForgeMod.WATER_TYPE.get()) {
                /* Don't shoot through fluids (we're not gonna hit anyway).
                 * Pushing logic is insane, see updateFluidHeightAndDoFluidPushing in Entity class. It pushes in relation to fluid height.
                 * TODO: Maybe in the future.
                 */
                return false;
            }
            v = v.scale(friction).add(ARROW_GRAVITY);
        }
        // check one last loop iteration. Take care not to collide if we overshoot player.
        lastPos = p;
        p = p.add(v);
        BlockPos blockpos = new BlockPos(p);
        BlockState blockstate = this.level.getBlockState(blockpos);
        //if (!blockstate.isAir()) {
        //    VoxelShape voxelshape = blockstate.getCollisionShape(this.level, blockpos);
        //    if (!doesArrowHitTarget(voxelshape, p, lastPos, blockpos)) {
        //        return false;
        //    }
        //}
        boolean hitsWall = doesAnyCornerHitWall(p, lastPos, getTargetPoint(), ARROW_BOUNDING_BOX_RADIUS);

        Vec3 arrowToTarget = getTargetPoint().subtract(p);
        Vec3 arrowDirection = p.subtract(lastPos);
        double distanceToTarget = arrowDirection.dot(arrowToTarget);
        //ExampleMod.LOGGER.info("arrow to target: " + getTargetPoint().subtract(p));
        //ExampleMod.LOGGER.info("distance to target" + distanceToTarget);

        return !hitsWall;
    }

    private boolean doesAnyCornerHitWall(Vec3 p, Vec3 previousPos, Vec3 targetPos, float aabbRadius) {
        /* TODO: What the hell is this code? I only want to iterate over all AABB corners of the arrow and check each for
         * collision except one. There has to be be a better way.
         */
        Vec3 arrowDirection = p.subtract(previousPos);
        byte cornerWeDontNeedToCheck = 0;
        if (arrowDirection.x < 0) {
            cornerWeDontNeedToCheck += 4;
        }
        if (arrowDirection.y < 0) {
            cornerWeDontNeedToCheck += 2;
        }
        if (arrowDirection.z < 0) {
            cornerWeDontNeedToCheck += 1;
        }

        for(byte i = 0; i < 8; i++) {
            if (i == cornerWeDontNeedToCheck) {
                continue;
            }
            int x = -1 + 2*((i & 0x4)>>2);
            int y = 0 + 2*((i & 0x2)>>1);
            int z = -1 + 2*(i & 0x1);
            Vec3 arrowCornerBefore = previousPos.add(aabbRadius * x, aabbRadius * y, aabbRadius * z);
            Vec3 arrowCornerAfter = p.add(aabbRadius * x, aabbRadius * y, aabbRadius * z);
            if (doesRaycastHitWall(arrowCornerAfter, arrowCornerBefore, targetPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean doesRaycastHitWall(Vec3 p, Vec3 previousPos, Vec3 targetPos) {
        var context = new ClipContext(previousPos, p, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null);
        BlockHitResult hitsWall = BlockGetter.traverseBlocks(previousPos, p, context, ((clipContext, blockPos) -> {
            var solidState = level.getBlockState(blockPos);
            var solidShape = clipContext.getBlockShape(solidState, level, blockPos);
            //ExampleMod.LOGGER.info("solid shape at: " + blockPos + " is " + solidShape);
            BlockHitResult solidHit = level.clipWithInteractionOverride(previousPos, p, blockPos, solidShape, solidState);

            var liquidShape = clipContext.getFluidShape(level.getFluidState(blockPos), level, blockPos);
            BlockHitResult liquidHit = liquidShape.clip(previousPos, p, blockPos);

            if (solidHit == null && liquidHit == null) {
                return null; // returning null gets the next block, because nulls are a design pattern
            } else if (solidHit == null && liquidHit != null) {
                //return liquidHit.getLocation().distanceToSqr(lastPos);
                return liquidHit;
            } else if (solidHit != null && liquidHit == null) {
                //return solidHit.getLocation().distanceToSqr(lastPos);
                return solidHit;
            } else {
                //return Math.min(solidHit.getLocation().distanceToSqr(lastPos), liquidHit.getLocation().distanceToSqr(lastPos));
                double d1 = solidHit.getLocation().distanceTo(previousPos);
                double d2 = liquidHit.getLocation().distanceTo(previousPos);
                return d1 > d2 ? solidHit : liquidHit;
            }
        }), (clipContext) -> {
            Vec3 vec3 = clipContext.getFrom().subtract(clipContext.getTo());
            return BlockHitResult.miss(clipContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(clipContext.getTo()));
        });
        if (hitsWall.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (previousPos.distanceToSqr(hitsWall.getLocation()) < previousPos.distanceToSqr(targetPos)) {
            return true;
        } else {
            return false;
        }
    }

    // TODO: what to do with other method of same name in superclass? That one should never be called.
    public void performRangedAttack(float p_32142_, Shot shot) {
        ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem)));
        AbstractArrow abstractarrow = this.getArrow(itemstack, p_32142_);
        if (this.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
            abstractarrow = ((net.minecraft.world.item.BowItem)this.getMainHandItem().getItem()).customArrow(abstractarrow);
        Vec3 shootingVector = shot.v;
        //Vec3 v = calculateShotsSortedByImpactTimeWithDrag(target, abstractarrow.position()).v;
        final float DISPERSION = 0;
        abstractarrow.shoot(shootingVector.x, shootingVector.y, shootingVector.z, ARROW_SPEED, DISPERSION);
        lastArrow = abstractarrow;
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addFreshEntity(abstractarrow);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new MyAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new MyAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new MyAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }


    @Override
    final public void reassessWeaponGoal() {
        // AbstractSkeleton calls this method during initialization, which is unsound, so we need to check if private fields are ok.
        boolean initializationOk = this.bowGoal != null && this.meleeGoal != null;
        if (this.level != null && !this.level.isClientSide && initializationOk) {
            this.goalSelector.removeGoal(meleeGoal);
            this.goalSelector.removeGoal(bowGoal);
            ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem));
            if (itemstack.is(Items.BOW)) {
                int i = 20;
                if (this.level.getDifficulty() != Difficulty.HARD) {
                    i = 40;
                }

                this.bowGoal.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, bowGoal);
            } else {
                this.goalSelector.addGoal(4, meleeGoal);
            }

        }
    }

    private Shot[] calculateShotsSortedByImpactTime(LivingEntity enemy, Vec3 arrowPosition) {
        boolean hasGravity = !enemy.isOnGround() && !enemy.isNoGravity();
        Vec3 g_player = hasGravity ? new Vec3(0, -0.05, 0) : Vec3.ZERO;
        //g_player = Vec3.ZERO;
        Vec3 g = ARROW_GRAVITY;
        Vec3 vpWithYComponent = CommonMCEvents.playerPosAfter.subtract(CommonMCEvents.playerPosBefore);
        Vec3 vp = new Vec3(vpWithYComponent.x, 0, vpWithYComponent.z);
        //ExampleMod.LOGGER.info("good movement?: " + CommonMCEvents.playerPosValid + ", got movement: " + vp);
        Vec3 p0 = enemy.getEyePosition();
        Vec3 a0 = arrowPosition;
        //ExampleMod.LOGGER.info("Distance to target: " + (p0.subtract(a0)).length());
        final double FRICTION = 0.99;
        final double d = FRICTION;
        final double D = Math.pow((d - 1), 2);
        Vec3 c = p0.subtract(a0).add(g.scale(1/D));
        Vec3 b = vp.add(g_player.scale(-0.5)).add(g.scale(d / D)).subtract(g.scale(1/D));
        final double s = ARROW_SPEED * ARROW_SPEED;

        double p2 = g.lengthSqr() / D - s;
        double p1 = 2*s - 2*g.dot(c);
        double p11 = -2*g.dot(b);
        double p21 = -g_player.dot(g);
        double z = D * g_player.lengthSqr() / 4;
        double y = D * g_player.dot(b);
        double x = D * g_player.dot(c) + D * b.lengthSqr();
        double w = 2 * D * b.dot(c);
        double v = D * c.lengthSqr() - s;
        //ExampleMod.LOGGER.info("Vars: p2 = " + p2 + ",  p1 = " + p1 + ",  p11 = " + p11+ ", p21 = " + p21+ ", z = " + z + ", y = " + y + ", x = " + x + ", w = " + w + ", v = " + v + ", d = " + d);

        final double MAX_T = 500; //TODOFigure out mathematical max time an arrow can fly for given player coordinates and set it to that.
        double slowerSolution = findZero(MAX_T, p2, p1, p11, p21, z, y, x, w, v, d, Optional.empty());
        Optional firstRoot = Optional.of(slowerSolution-0.001); //TODO: pick good values for these epsilons
        double fasterSolution = findZero(slowerSolution-0.1, p2, p1, p11, p21, z, y, x, w, v, d, firstRoot);
        if (slowerSolution > 0 && fasterSolution > 0) {
            Vec3 slowerShot = calculateSpeedVector(slowerSolution, p0, a0, vp, g, g_player, d, D);
            Vec3 fasterShot = calculateSpeedVector(fasterSolution, p0, a0, vp, g, g_player, d, D);
            return new Shot[]{new Shot(fasterSolution, fasterShot), new Shot(slowerSolution, slowerShot)};
        } else if (slowerSolution > 0) {
            Vec3 slowerShot = calculateSpeedVector(slowerSolution, p0, a0, vp, g, g_player, d, D);
            return new Shot[]{new Shot(slowerSolution, slowerShot)};
        } else {
            return new Shot[]{};
        }
    }

    private Vec3 calculateSpeedVector(double t, Vec3 p0, Vec3 a0, Vec3 vp, Vec3 g, Vec3 g_player, double d, double D) {
        Vec3 va = (p0.subtract(a0).add(vp.scale(t)).add(g_player.scale(t*t/2 - t/2)).subtract(g.scale((Math.pow(d, t) - d*t  + t - 1)/D))).scale((d-1)/(Math.pow(d, t) - 1));
        return va;
    }

    private double findZero(double t0, double p2, double p1, double p11, double p21, double z, double y, double x, double w, double v, double d, Optional<Double> firstSolution) {
        double t = t0;
        double ft = eval(t, p2, p1, p11, p21, z, y, x, w, v, d, firstSolution); //f(t)
        double ft_; //f(t)'
        int i = 0;
        while (Math.abs(ft) > 10e-10) {
            ft_ = evalDerivative(t, p2, p1, p11, p21, z, y, x, w, d);
            if (firstSolution.isPresent()) {
                ft_ = (ft_ * (t - firstSolution.get())) / Math.pow(t - firstSolution.get(), 2) - ft / (t - firstSolution.get());
            }
            t = t - ft / ft_;
            //ExampleMod.LOGGER.info("Iteration " + i + ", t = " + t + ", eval " + ft + ", derivative = " + ft_);
            ft = eval(t, p2, p1, p11, p21, z, y, x, w, v, d, firstSolution);
            i++;
            if (i > 1000) {
                break;
            }
        }
        return t;
    }

    private double eval(double t, double p2, double p1, double p11, double p21, double z, double y, double x, double w, double v, double d, Optional<Double> firstSolution) {
        double dPowT = Math.pow(d, t);
        double result = Math.pow(d, 2*t) * p2 + dPowT * p1 + t*dPowT*p11 + t*t*dPowT*p21 + Math.pow(t, 4)*z
            + Math.pow(t, 3)*y + Math.pow(t, 2)*x + t*w + v;
        if (firstSolution.isPresent()) {
            result = result / (t - firstSolution.get());
        }
        return result;
    }

    private double evalDerivative(double t, double p2, double p1, double p11, double p21, double z, double y, double x, double w, double d) {
        double dPowT = Math.pow(d, t);
        //double result = 2*Math.pow(d, 2*t) * p2 + dPowT * p1 + t*dPowT*p11 + dPowT*p11 + Math.pow(t, 2)*dPowT*p21 + 2*t*dPowT*p21 + 4*Math.pow(t, 3) * z
        double result = Math.log(d) * (2*Math.pow(d, 2*t)*p2 + dPowT*p1 + t*dPowT*p11 + Math.pow(t, 2)*dPowT*p21) + dPowT*p11 + 2*t*dPowT*p21 + 4*Math.pow(t, 3)*z
                + 3*Math.pow(t, 2)*y + 2*t*x + w;
        return result;
    }

    private Vec3 calculateShot(LivingEntity enemy, AbstractArrow arrow, double speed) {
        boolean hasGravity = !enemy.isOnGround() && !enemy.isNoGravity();
        Vec3 g_player = hasGravity ? new Vec3(0, -0.05, 0) : Vec3.ZERO;
        //Vec3 g  = g_player.subtract(new Vec3(0, -0.05, 0));
        Vec3 g = new Vec3(0, 0.05, 0);
        Vec3 vp = CommonMCEvents.playerPosAfter.subtract(CommonMCEvents.playerPosBefore);
        ExampleMod.LOGGER.info("got movement:");
        ExampleMod.LOGGER.info(String.valueOf(CommonMCEvents.playerPosValid));
        ExampleMod.LOGGER.info(vp.toString());
        Vec3 p0 = enemy.getEyePosition().subtract(new Vec3(0,0.33,0));
        Vec3 a0 = arrow.position();

        double speedSqr = speed * speed;
        double a = 0.25d * g.lengthSqr();
        double b = 2 * vp.dot(g) + 0.5d * g.lengthSqr();
        double c = vp.lengthSqr() + p0.dot(g) - a0.dot(g) - speedSqr + g.dot(vp) + 0.25d*g.lengthSqr();
        double d = 2*vp.dot(p0) - 2*vp.dot(a0) + g.dot(p0) - g.dot(a0);
        double e = p0.lengthSqr() + a0.lengthSqr() - 2*p0.dot(a0);
        double[] ap = {Math.pow(a, 2), Math.pow(a, 3), Math.pow(a, 4)};
        double[] bp = {Math.pow(b, 2), Math.pow(b, 3), Math.pow(b, 4)};
        double[] cp = {Math.pow(c, 2), Math.pow(c, 3), Math.pow(c, 4)};
        double[] dp = {Math.pow(d, 2), Math.pow(d, 3), Math.pow(d, 4)};
        double[] ep = {Math.pow(e, 2), Math.pow(e, 3), Math.pow(e, 4)};
        double D = 256*ap[1]*ep[1] - 192*ap[0]*b*d*ep[0] - 128*ap[0]*cp[0]*ep[0] +
                144*ap[0]*c*dp[0]*e - 27*ap[0]*dp[2] + 144*a*bp[0]*c*ep[0] - 6*a*bp[0]*dp[0]*e -
                80*a*b*cp[0]*d*e + 18*a*b*c*dp[1] + 16*a*cp[2]*e -
                4*a*cp[1]*dp[0] - 27*bp[2]*ep[0] + 18*bp[1]*c*d*e - 4*bp[1]*dp[1] - 4*bp[0]*cp[1]*e + bp[0]*cp[0]*dp[0];

        double p = (8*a*c - 3*bp[0])/(8*ap[0]);
        double q = (bp[1] - 4*a*b*c + 8*ap[0]*d)/(8*ap[1]);

        double D0 = cp[0] - 3*b*d + 12*a*e;
        double D1 = 2*cp[1] - 9*b*c*d + 27*bp[0]*e + 27*a*dp[0] - 72*a*c*e;

        double S;
        if (D > 0) {
            double psi = Math.acos(D1 / (2*Math.sqrt(Math.pow(D0, 3))));
            double S_real = 0.5*Math.sqrt(-2/3d*p + 2/(3d*a) * Math.sqrt(D0)*Math.cos(psi/3d));
            S = S_real;
        } else {
            double disc_Q = -27*D;
            double Q = Math.pow((D1 + Math.sqrt(disc_Q))/2, 1d/3d);
            double S1 = 0.5*Math.sqrt(-(2d/3d)*p + 1d/(3*a) * (Q + D0/Q));
            S = S1;

        }

        ArrayList<Double> validSolutions = new ArrayList<Double>();
        double disc_SPlus = -4*S*S - 2*p + q/S;
        double disc_SMinus = -4*S*S - 2*p - q/S;
        if (disc_SPlus > 0) {
            double x12_part = 0.5 * Math.sqrt(disc_SPlus);
            double x1 = -b/(4*a) - S + x12_part;
            double x2 = -b/(4*a) - S - x12_part;
            validSolutions.add(x1);
            validSolutions.add(x2);
        }
        if (disc_SMinus > 0) {
            double x34_part = 0.5 * Math.sqrt(disc_SMinus);
            double x3 = -b/(4*a) + S + x34_part;
            double x4 = -b/(4*a) + S - x34_part;
            validSolutions.add(x3);
            validSolutions.add(x4);
        }

        double best_t = Double.MAX_VALUE;
        for (var s : validSolutions) {
            if (s > 0 && s < best_t) {
                best_t = s;
            }
        }
        double t = best_t;
        ExampleMod.LOGGER.info("Best t: " + t);

        Vec3 va = p0.subtract(a0).add(vp.scale(t)).add(g.scale(1/2d * t*t)).scale(1/t);
        return va;
    }
}
