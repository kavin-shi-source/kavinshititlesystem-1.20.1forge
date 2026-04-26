package com.kavinshi.playertitle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("null")
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ChatHeadRenderer {
    private static final int MAX_TRACKED = 20;
    private static final ConcurrentLinkedQueue<TrackedMessage> tracked = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        UUID uuid = extractUUID(event.getMessage());
        if (uuid != null) {
            tracked.add(new TrackedMessage(uuid));
            while (tracked.size() > MAX_TRACKED) tracked.poll();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || tracked.isEmpty()) return;
        if (mc.getConnection() == null) return;

        List<TrackedMessage> snapshot = new ArrayList<>(tracked);
        GuiGraphics g = event.getGuiGraphics();
        int chatBottom = mc.getWindow().getGuiScaledHeight() - 40;
        int lineH = 9;
        int visibleCount = Math.min(snapshot.size(), 10);
        int drawIndex = 0;

        for (int i = 0; i < visibleCount; i++) {
            TrackedMessage msg = snapshot.get(i);
            if (msg.uuid.equals(mc.player.getUUID())) continue;

            PlayerInfo info = mc.getConnection().getPlayerInfo(msg.uuid);
            if (info == null) continue;

            int y = chatBottom - (drawIndex + 1) * lineH;
            renderHead(g, info, 2, y);
            drawIndex++;
        }
    }

    private static void renderHead(GuiGraphics g, PlayerInfo info, int x, int y) {
        try {
            g.blit(info.getSkinLocation(), x, y, 8, 8, 8f, 8f, 8, 8, 64, 64);
            g.blit(info.getSkinLocation(), x, y, 8, 8, 40f, 8f, 8, 8, 64, 64);
        } catch (Exception ignored) {}
    }

    private static UUID extractUUID(Component component) {
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            HoverEvent.EntityTooltipInfo info = hover.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (info != null && info.id != null) {
                return info.id;
            }
        }
        for (Component sibling : component.getSiblings()) {
            UUID result = extractUUID(sibling);
            if (result != null) return result;
        }
        return null;
    }

    private static final class TrackedMessage {
        final UUID uuid;

        TrackedMessage(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
