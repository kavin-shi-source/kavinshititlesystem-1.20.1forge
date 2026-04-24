package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.ModConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * 网络处理器，负责初始化网络通道和注册所有数据包类型。
 * 使用Minecraft Forge的SimpleChannel实现客户端-服务器通信。
 */
public class NetworkHandler {
    private static final Logger LOGGER = LogManager.getLogger(NetworkHandler.class);
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;
    private static boolean packetsRegistered = false;

    public static void init() {
        if (CHANNEL != null) {
            LOGGER.warn("Network channel already initialized");
            return;
        }

        CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        LOGGER.info("Initialized network channel for {}", ModConstants.MOD_ID);
    }

    public static SimpleChannel getChannel() {
        if (CHANNEL == null) {
            throw new IllegalStateException("Network channel not initialized. Call init() first.");
        }
        return CHANNEL;
    }

    public static void registerPackets() {
        if (packetsRegistered) {
            LOGGER.warn("Network packets already registered");
            return;
        }
        packetsRegistered = true;

        LOGGER.info("Registering network packets for {}", ModConstants.MOD_ID);

        CHANNEL.registerMessage(packetId++,
            SyncPlayerTitlesPacket.class,
            SyncPlayerTitlesPacket::encode,
            SyncPlayerTitlesPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId++,
            RequestSyncPacket.class,
            RequestSyncPacket::encode,
            RequestSyncPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId++,
            TitleUpdatePacket.class,
            TitleUpdatePacket::encode,
            TitleUpdatePacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId++,
            SyncTitleRegistryPacket.class,
            SyncTitleRegistryPacket::encode,
            SyncTitleRegistryPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId++,
            CustomTitleUpdatePacket.class,
            CustomTitleUpdatePacket::encode,
            CustomTitleUpdatePacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId++,
            ClusterSyncPacket.class,
            ClusterSyncPacket::encode,
            ClusterSyncPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        LOGGER.info("Registered {} packet types", packetId);
    }

    private static <T extends AbstractPacket> void handlePacket(T packet, Supplier<NetworkEvent.Context> ctxSupplier, java.util.function.Consumer<NetworkEvent.Context> handler) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            try {
                handler.accept(ctx);
            } catch (Exception e) {
                LOGGER.error("Error handling packet {}", packet.getClass().getSimpleName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
}
