package com.example.hardai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class MyCreeper extends Creeper {
    public static final double FOLLOW_RANGE = 80d;
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
