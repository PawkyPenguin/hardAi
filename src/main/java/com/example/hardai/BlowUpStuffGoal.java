package com.example.hardai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class BlowUpStuffGoal extends Goal {
    private BlockEntity target = null;
    private final Creeper me;

    public BlowUpStuffGoal(Creeper me) {
        this.me = me;
    }

    @Override
    public boolean canUse() {
        this.findTarget();
        return this.target != null;
    }

    private BlockEntity findTarget() {
        var map = CommonMCEvents.chests;
        var chests = map.keys().asIterator();
        while (chests.hasNext()) {
            var c = chests.next();
            if (c.getBlockPos().distToCenterSqr(me.position()) < Math.pow(MyCreeper.FOLLOW_RANGE,2)) {
                return c;
            }
        };
        return null;
    }
}
