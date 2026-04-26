package com.kavinshi.playertitle.velocity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

public class ChatMessagePacket {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 16;

    private final UUID playerId;
    private final String playerName;
    private final String serverName;
    private final String message;

    public ChatMessagePacket(UUID playerId, String playerName, String serverName, String message) {
        this.playerId = playerId;
        this.playerName = playerName != null && playerName.length() > MAX_NAME_LENGTH
            ? playerName.substring(0, MAX_NAME_LENGTH) : playerName;
        this.serverName = serverName;
        this.message = sanitize(message);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getMessage() {
        return message;
    }

    public byte[] toBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(playerId.getMostSignificantBits());
            dos.writeLong(playerId.getLeastSignificantBits());
            dos.writeUTF(playerName != null ? playerName : "");
            dos.writeUTF(serverName != null ? serverName : "");
            dos.writeUTF(message);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize ChatMessagePacket", e);
        }
    }

    public static ChatMessagePacket fromBytes(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            UUID playerId = new UUID(dis.readLong(), dis.readLong());
            String playerName = dis.readUTF();
            String serverName = dis.readUTF();
            String message = dis.readUTF();
            return new ChatMessagePacket(playerId, playerName, serverName, message);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize ChatMessagePacket", e);
        }
    }

    private static String sanitize(String message) {
        if (message == null) return "";
        String trimmed = message.length() > MAX_MESSAGE_LENGTH
            ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\u00A7') {
                i++;
            } else if (c >= ' ' || c == '\n') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
