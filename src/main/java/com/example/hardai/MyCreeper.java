package com.example.hardai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class MyCreeper extends Creeper {
    public static final double FOLLOW_RANGE = 80d;

    private static Block[] detonationTargets = {Blocks.CHEST, Blocks.TRAPPED_CHEST};

    public static boolean isBlockIWantToDetonate(Block b) {
        for (var target : detonationTargets) {
            if (b == target) {
                return true;
            }
        }
        return false;
    }

    public MyCreeper(EntityType<? extends Creeper> p_32278_, Level p_32279_) {
        super(p_32278_, p_32279_);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        ExampleMod.LOGGER.info("registering: " + this);
        this.goalSelector.addGoal(1, new BlowUpStuffGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

}
