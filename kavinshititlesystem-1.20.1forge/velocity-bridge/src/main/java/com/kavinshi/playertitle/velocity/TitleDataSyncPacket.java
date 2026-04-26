package com.kavinshi.playertitle.velocity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TitleDataSyncPacket {

    private final UUID playerId;
    private final String playerName;
    private final int equippedTitleId;
    private final String titleName;
    private final int titleColor;
    private final String serverName;
    private final String heading;

    public TitleDataSyncPacket(UUID playerId, String playerName, String serverName, int equippedTitleId,
                              String titleName, int titleColor, String heading) {
        this.playerId = playerId;
        this.playerName = playerName != null ? playerName : "";
        this.serverName = serverName != null ? serverName : "";
        this.equippedTitleId = equippedTitleId;
        this.titleName = titleName != null ? titleName : "";
        this.titleColor = titleColor;
        this.heading = heading != null ? heading : "";
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getEquippedTitleId() { return equippedTitleId; }
    public String getTitleName() { return titleName; }
    public int getTitleColor() { return titleColor; }
    public String getHeading() { return heading; }
    public String getServerName() { return serverName; }

    public byte[] toBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        try (DataOutputStream out = new DataOutputStream(baos)) {
            writeUUID(out, playerId);
            writeString(out, playerName);
            writeString(out, serverName);
            out.writeInt(equippedTitleId);
            writeString(out, titleName);
            out.writeInt(titleColor);
            writeString(out, heading);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode TitleDataSyncPacket", e);
        }
        return baos.toByteArray();
    }

    public static TitleDataSyncPacket fromBytes(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            UUID playerId = readUUID(in);
            String playerName = readString(in);
            String serverName = readString(in);
            int equippedTitleId = in.readInt();
            String titleName = readString(in);
            int titleColor = in.readInt();
            String heading = readString(in);
            return new TitleDataSyncPacket(
                playerId, playerName, serverName, equippedTitleId, titleName, titleColor, heading
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode TitleDataSyncPacket", e);
        }
    }

    private static void writeUUID(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUUID(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65535) throw new IOException("String too long for writeString: " + bytes.length);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readShort() & 0xFFFF;
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
