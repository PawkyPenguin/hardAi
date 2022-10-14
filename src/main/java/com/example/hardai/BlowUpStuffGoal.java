package com.example.hardai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class BlowUpStuffGoal extends Goal {
    private BlockEntity target = null;
    private Path pathToChest = null;
    private CommonMCEvents.MutableBool isTargetValid;
    private final Creeper me;

    private final double DETONATE_RANGE_SQR = 9;

    private int pathRecalculationMaxInterval = 30;
    private int pathRecalculationCooldown = 0;

    // TODO: do i gotta save this goal somehow?
    public BlowUpStuffGoal(Creeper me) {
        this.me = me;
    }

    @Override
    public void start() {
        super.start();
        findTarget();
        if (target != null) {
            updatePath();
            me.getNavigation().moveTo(this.pathToChest, 1);
            me.getLookControl().setLookAt(getBlockPosVec());
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.target = null;
        this.pathToChest = null;
        this.pathRecalculationCooldown = 0;
        me.getNavigation().stop();
        //TODO: Look somewhere random
    }

    @Override
    public boolean canUse() {
        findTarget();
        return this.target != null;
    }

    private Vec3 getBlockPosVec() {
        var pos = target.getBlockPos();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && isTargetValid.get() && canReach();
    }

    private void findTarget() {
        var map = CommonMCEvents.chests;
        var chests = map.keys().asIterator();
        while (chests.hasNext()) {
            var c = chests.next();
            if (c.getBlockPos().distToCenterSqr(me.position()) < Math.pow(MyCreeper.FOLLOW_RANGE,2)) {
                this.target = c;
                this.isTargetValid = map.get(c);
                this.pathToChest = me.getNavigation().createPath(c.getBlockPos(), 0);
            }
        }
    }

    private boolean canReach() {
        if (pathToChest == null) {
            return false;
        }
        Node node = pathToChest.getEndNode();
        if (node == null) {
            return false;
        }
        int i = node.x - target.getBlockPos().getX();
        int j = node.z - target.getBlockPos().getZ();
        return (double)(i * i + j * j) < DETONATE_RANGE_SQR;
    }

    private void updatePath() {
        pathToChest = me.getNavigation().createPath(target.getBlockPos(), 0);
    }

    @Override
    public void tick() {
        ExampleMod.LOGGER.info("ticking blowing up");
        if (--pathRecalculationCooldown <= 0) {
            pathRecalculationCooldown = pathRecalculationMaxInterval;
            updatePath();
            me.getNavigation().moveTo(pathToChest, 1);
        }
        if (this.target != null && this.isTargetValid.get()) {
            var posVec = getBlockPosVec();
            boolean isNear = me.distanceToSqr(posVec) <= DETONATE_RANGE_SQR;
            if (isNear) {
                me.setSwellDir(1);
            }
        } else if (this.target != null && !this.isTargetValid.get()) {
            me.setSwellDir(-1);
        }
    }
}
