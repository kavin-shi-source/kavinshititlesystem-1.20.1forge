package com.kavinshi.playertitle.handler;

import com.kavinshi.playertitle.ModConstants;
import com.kavinshi.playertitle.bootstrap.RewriteBootstrap;
import com.kavinshi.playertitle.network.ChatMessagePacket;
import com.kavinshi.playertitle.player.TitleCapability;
import com.kavinshi.playertitle.sync.ClusterMode;
import com.kavinshi.playertitle.title.MinecraftColors;
import com.kavinshi.playertitle.title.TitleDisplayHelper;
import com.kavinshi.playertitle.title.TitleRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)

@SuppressWarnings("null")
public final class ChatHandler {
    private static final ResourceLocation CHAT_CHANNEL = ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "chat");

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        TitleRegistry registry = RewriteBootstrap.getInstance().getTitleRegistry();
        TitleCapability.get(player).ifPresent(state -> {
            MutableComponent titleComponent = TitleDisplayHelper.buildTitleComponent(state, registry);
            if (titleComponent == null) return;

            ClusterMode mode = RewriteBootstrap.getInstance().getClusterConfig().getMode();
            if (mode == ClusterMode.VELOCITY) {
                sendCrossServerChat(player, event.getMessage().getString());
            }

            Component rawDisplayName = player.getDisplayName();
            if (rawDisplayName == null) rawDisplayName = Component.literal(player.getScoreboardName());
            
            String serverName = RewriteBootstrap.getInstance().getClusterConfig().getServerName();
            String colorCode = MinecraftColors.toSectionCode(RewriteBootstrap.getInstance().getClusterConfig().getServerNameColor());
            MutableComponent serverComponent = Component.literal(colorCode + "[" + serverName + "]\u00A7r");

            MutableComponent prefix = Component.literal("")
                    .append(serverComponent)
                    .append(Component.literal("-"));
            
            String heading = state.getHeading();
            if (!heading.isEmpty()) {
                prefix.append(Component.literal("[" + heading + "]"));
            }
            
            if (titleComponent != null) {
                prefix.append(titleComponent);
            }
            
            prefix.append(Component.literal(" "));

            // Update the message in the event instead of cancelling it to preserve signatures
            event.setMessage(prefix.append(event.getMessage()));
        });
    }

    private static void sendCrossServerChat(ServerPlayer player, String rawMessage) {
        String serverName = RewriteBootstrap.getInstance().getClusterConfig().getServerName();
        ChatMessagePacket packet = new ChatMessagePacket(
            player.getUUID(),
            player.getGameProfile().getName(),
            serverName,
            rawMessage
        );
        byte[] data = packet.toBytes();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(data);
        ClientboundCustomPayloadPacket payload = new ClientboundCustomPayloadPacket(CHAT_CHANNEL, buf);
        player.connection.send(payload);
    }
}
