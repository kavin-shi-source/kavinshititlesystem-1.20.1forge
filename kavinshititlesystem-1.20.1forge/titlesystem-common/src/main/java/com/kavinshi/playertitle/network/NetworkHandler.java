package com.kavinshi.playertitle.network;

import com.kavinshi.playertitle.ModConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 称号系统的网络处理器。
 * 负责注册和管理所有服务器-客户端通信包。
 * 
 * 使用Forge的SimpleChannel实现网络通信。
 * 支持以下包类型：
 * 1. 同步玩家称号数据（服务器→客户端）
 * 2. 请求同步数据（客户端→服务器）
 * 3. 图标更新通知（双向）
 * 4. 称号装备状态变更（客户端→服务器）
 */
public class NetworkHandler {
    private static final Logger LOGGER = LogManager.getLogger(NetworkHandler.class);
    
    // 网络协议版本
    private static final String PROTOCOL_VERSION = "1.0.0";
    
    // 网络通道实例
    private static SimpleChannel CHANNEL;
    
    // 包ID计数器
    private static int packetId = 0;
    
    /**
     * 初始化网络通道。
     * 必须在服务器和客户端都调用此方法进行初始化。
     */
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
    
    /**
     * 获取网络通道实例。
     * 
     * @return 网络通道
     */
    public static SimpleChannel getChannel() {
        if (CHANNEL == null) {
            throw new IllegalStateException("Network channel not initialized. Call init() first.");
        }
        return CHANNEL;
    }
    
    /**
     * 注册所有网络包。
     * 服务器和客户端应分别注册所需的包处理器。
     */
    public static void registerPackets() {
        LOGGER.info("Registering network packets for {}", ModConstants.MOD_ID);
        
        // 注册包类型
        registerPacket(SyncPlayerTitlesPacket.class, SyncPlayerTitlesPacket::new);
        registerPacket(RequestSyncPacket.class, RequestSyncPacket::new);
        registerPacket(TitleUpdatePacket.class, TitleUpdatePacket::new);
        
        LOGGER.info("Registered {} packet types", packetId);
    }
    
    /**
     * 注册单个包类型。
     * 
     * @param packetClass 包类
     * @param decoder 包解码器
     * @param <T> 包类型
     */
    private static <T extends AbstractPacket> void registerPacket(Class<T> packetClass, PacketDecoder<T> decoder) {
        CHANNEL.registerMessage(
            packetId++,
            packetClass,
            (packet, buffer) -> packet.encode(buffer),
            decoder::decode,
            (packet, ctxSupplier) -> packet.handle(ctxSupplier.get())
        );
    }
    
    /**
     * 包解码器函数接口。
     */
    @FunctionalInterface
    public interface PacketDecoder<T extends AbstractPacket> {
        T decode(FriendlyByteBuf buffer);
    }
}