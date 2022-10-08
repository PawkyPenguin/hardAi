package com.example.hardai;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonMCEvents {

    @SubscribeEvent
    public static void checkSpawn (LivingSpawnEvent.CheckSpawn event) {
        event.getEntity().remove(Entity.RemovalReason.DISCARDED);
        event.setResult(Event.Result.DENY);
    }
}
