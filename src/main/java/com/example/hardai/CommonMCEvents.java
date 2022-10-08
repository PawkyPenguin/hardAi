package com.example.hardai;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonMCEvents {
    private static final IEventBus mcBus = Mod.EventBusSubscriber.Bus.FORGE.bus().get();

    @SubscribeEvent
    public static void checkSpawn (LivingSpawnEvent.CheckSpawn event) {
        var mob = event.getEntity();
        var type = mob.getType();
        if(type == EntityType.SKELETON) {
            mob.remove(Entity.RemovalReason.DISCARDED);
            event.setResult(Event.Result.DENY);
            Player player = null;
            ItemStack item = null;
            boolean expandAABBToMinusY = false;
            boolean spawnWithUpwardOffset = true;
            ServerLevel level = mob.getServer().getLevel(mob.level.dimension());
            ExampleMod.MY_SKELETON.get().spawn(level, item, player, event.getEntity().blockPosition(), MobSpawnType.NATURAL, spawnWithUpwardOffset, expandAABBToMinusY);
            EntityType.AXOLOTL.spawn(level, item, player, event.getEntity().blockPosition(), MobSpawnType.NATURAL, spawnWithUpwardOffset, expandAABBToMinusY);
        }
    }
}
