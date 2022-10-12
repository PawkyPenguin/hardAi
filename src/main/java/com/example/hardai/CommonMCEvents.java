package com.example.hardai;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static Vec3 playerPosBefore;
    public static Vec3 playerPosAfter;
    public static boolean playerPosValid;

    @SubscribeEvent
    public static void listenMovement(TickEvent.PlayerTickEvent p) {
        //ExampleMod.LOGGER.info("Player");
        //ExampleMod.LOGGER.info(p.phase.toString());
        //ExampleMod.LOGGER.info(p.player.position().toString());
        if (p.phase == TickEvent.Phase.START) {
            playerPosBefore = p.player.position();
            playerPosValid = false;
        } else {
            playerPosAfter = p.player.position();
            playerPosValid = true;

        }
    }

    public static class CachedBlockEntity {
        BlockEntity wrapper;
        private boolean isValid = true;

        CachedBlockEntity(BlockEntity toCache) {
            wrapper = toCache;
        }

        public boolean isValid() {
            return isValid;
        }

        public void invalidate() {
            isValid = false;
        }
    }

    public static class MutableBool extends AtomicBoolean {
        public MutableBool(boolean initialValue) {
            super(initialValue);
        }
    }

    //public static List<CachedBlockEntity> chests = Collections.synchronizedList(new ArrayList());
    public static final ConcurrentHashMap<BlockEntity, MutableBool> chests = new ConcurrentHashMap();
    @SubscribeEvent
    public static void listenChunkLoad(ChunkEvent.Load ev) {
        ExampleMod.LOGGER.info("Loaded chunk");
        var allEntities = ev.getChunk().getBlockEntitiesPos();
        var allChests = allEntities.stream().flatMap(pos -> ev.getLevel().getBlockEntity(pos, BlockEntityType.CHEST).stream());
        for (var c : allChests.toList()) {
            chests.put(c, new MutableBool(true));
        }
        ExampleMod.LOGGER.info("Done reading chests in chunk");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void listenBlockPlace(BlockEvent.EntityPlaceEvent ev) {
        ExampleMod.LOGGER.info("Placed block");
        if (ev.isCanceled()) {
            return;
        }
        if (ev.getPlacedBlock().getBlock() == Blocks.CHEST) {
            BlockEntity chest = ev.getLevel().getBlockEntity(ev.getPos());
            ExampleMod.LOGGER.info("Placed chest: " + chest);
            chests.put(chest, new MutableBool(true));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void listenBlockRemove(BlockEvent.BreakEvent ev) {
        ExampleMod.LOGGER.info("Placed block");
        if (ev.isCanceled()) {
            return;
        }
        if (ev.getState().getBlock() == Blocks.CHEST) {
            BlockEntity chest = ev.getLevel().getBlockEntity(ev.getPos());
            ExampleMod.LOGGER.info("Removed chest: " + chest + ", map size: " + chests.size());
            var isValid = chests.get(chest);
            isValid.set(false);
            chests.remove(chest);
            ExampleMod.LOGGER.info("map size: " + chests.size());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void listenExplosion(ExplosionEvent.Detonate detonation) {
        ExampleMod.LOGGER.info("Placed block");
        if (detonation.isCanceled()) {
            return;
        }
        for (var c : detonation.getAffectedBlocks()) {
            var affectedBlock = detonation.getLevel().getBlockState(c).getBlock();
            if (MyCreeper.isBlockIWantToDetonate(affectedBlock)) {
                BlockEntity chest = detonation.getLevel().getBlockEntity(c);
                ExampleMod.LOGGER.info("Exploded chest: " + chest + ", map size: " + chests.size());
                var isValid = chests.get(chest);
                isValid.set(false);
                chests.remove(chest);
                ExampleMod.LOGGER.info("map size: " + chests.size());
            }
        }
    }
}
