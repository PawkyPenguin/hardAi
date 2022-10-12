package com.example.hardai;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
 //   @SubscribeEvent
 //   public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
 //       event.registerLayerDefinition(ModelLayers.SKELETON, SkeletonModel::createBodyLayer);
 //   }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExampleMod.MY_SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ExampleMod.MY_CREEPER.get(), CreeperRenderer::new);
    }
}
