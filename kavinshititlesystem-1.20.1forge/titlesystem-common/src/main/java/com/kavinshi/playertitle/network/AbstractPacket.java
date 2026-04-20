package com.kavinshi.playertitle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * 网络包基类。
 * 所有网络包都应继承此类并实现序列化、反序列化和处理逻辑。
 */
public abstract class AbstractPacket {
    private static final Logger LOGGER = LogManager.getLogger(AbstractPacket.class);
    
    /**
     * 将包数据编码到字节缓冲区。
     * 
     * @param buffer 字节缓冲区
     */
    public abstract void encode(FriendlyByteBuf buffer);
    
    /**
     * 处理接收到的包。
     * 
     * @param context 网络事件上下文
     */
    public abstract void handle(NetworkEvent.Context context);
    
    /**
     * 默认的包处理方法。
     * 子类可以覆盖此方法以提供自定义处理逻辑。
     */
    public static <T extends AbstractPacket> void handle(final T packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            try {
                packet.handle(ctx);
            } catch (Exception e) {
                LOGGER.error("Error handling packet {}", packet.getClass().getSimpleName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
    
    /**
     * 写入字符串到字节缓冲区（UTF-8编码）。
     */
    protected static void writeString(FriendlyByteBuf buffer, String str) {
        buffer.writeUtf(str, 32767);
    }
    
    /**
     * 从字节缓冲区读取字符串（UTF-8编码）。
     */
    protected static String readString(FriendlyByteBuf buffer) {
        return buffer.readUtf(32767);
    }
    
    /**
     * 写入UUID到字节缓冲区。
     */
    protected static void writeUUID(FriendlyByteBuf buffer, java.util.UUID uuid) {
        buffer.writeUUID(uuid);
    }
    
    /**
     * 从字节缓冲区读取UUID。
     */
    protected static java.util.UUID readUUID(FriendlyByteBuf buffer) {
        return buffer.readUUID();
    }
    
    /**
     * 写入Instant到字节缓冲区（毫秒时间戳）。
     */
    protected static void writeInstant(FriendlyByteBuf buffer, java.time.Instant instant) {
        buffer.writeLong(instant.toEpochMilli());
    }
    
    /**
     * 从字节缓冲区读取Instant。
     */
    protected static java.time.Instant readInstant(FriendlyByteBuf buffer) {
        return java.time.Instant.ofEpochMilli(buffer.readLong());
    }
}