package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.ModConstants;
import com.kavinshi.playertitle.player.TitleCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapabilityEventHandler {

    private static final ResourceLocation CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "title_data");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(CAPABILITY_ID, new TitleCapability.Provider(player.getUUID()));
        }
    }
}
