package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static volatile SimpleChannel CHANNEL;
    private static final AtomicInteger packetId = new AtomicInteger(0);
    private static volatile boolean packetsRegistered = false;

    private static final Object INIT_LOCK = new Object();

    public static void init() {
        if (CHANNEL != null) {
            LOGGER.warn("Network channel already initialized");
            return;
        }
        synchronized (INIT_LOCK) {
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
        }

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

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            SyncPlayerTitlesPacket.class,
            SyncPlayerTitlesPacket::encode,
            SyncPlayerTitlesPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            RequestSyncPacket.class,
            RequestSyncPacket::encode,
            RequestSyncPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            TitleUpdatePacket.class,
            TitleUpdatePacket::encode,
            TitleUpdatePacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            SyncTitleRegistryPacket.class,
            SyncTitleRegistryPacket::encode,
            SyncTitleRegistryPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            ClusterSyncPacket.class,
            ClusterSyncPacket::encode,
            ClusterSyncPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        CHANNEL.registerMessage(packetId.getAndIncrement(),
            HeadingRequestPacket.class,
            HeadingRequestPacket::encode,
            HeadingRequestPacket::new,
            (packet, ctxSupplier) -> handlePacket(packet, ctxSupplier, packet::handle)
        );

        LOGGER.info("Registered {} packet types", packetId.get());
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
