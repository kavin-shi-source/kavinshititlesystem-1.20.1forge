package com.kavinshi.playertitle.sync;

import com.kavinshi.playertitle.title.MinecraftColors;

public class ClusterConfig {
    private final ClusterMode mode;
    private final String velocityHost;
    private final int velocityPort;
    private final String channelName;
    private final String serverName;
    private final String serverNameColor;
    private final boolean enabled;

    private ClusterConfig(Builder builder) {
        this.mode = builder.mode;
        this.velocityHost = builder.velocityHost;
        this.velocityPort = builder.velocityPort;
        this.channelName = builder.channelName;
        this.serverName = builder.serverName;
        this.serverNameColor = builder.serverNameColor;
        this.enabled = builder.enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ClusterConfig config) {
        return new Builder(config);
    }

    public ClusterMode getMode() {
        return mode;
    }

    public String getVelocityHost() {
        return velocityHost;
    }

    public int getVelocityPort() {
        return velocityPort;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerNameColor() {
        return serverNameColor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Deprecated
    public static String toSectionCode(String colorName) {
        return MinecraftColors.toSectionCode(colorName);
    }

    @Deprecated
    public static int toHexColor(String colorName) {
        return MinecraftColors.toHexColor(colorName);
    }

    public static class Builder {
        private ClusterMode mode = ClusterMode.LOCAL;
        private String velocityHost = "localhost";
        private int velocityPort = 25577;
        private String channelName = "playertitle:sync";
        private String serverName = "unknown";
        private String serverNameColor = "gray";
        private boolean enabled = true;

        public Builder() {}

        public Builder(ClusterConfig config) {
            this.mode = config.mode;
            this.velocityHost = config.velocityHost;
            this.velocityPort = config.velocityPort;
            this.channelName = config.channelName;
            this.serverName = config.serverName;
            this.serverNameColor = config.serverNameColor;
            this.enabled = config.enabled;
        }

        public Builder mode(ClusterMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder velocityHost(String velocityHost) {
            this.velocityHost = velocityHost;
            return this;
        }

        public Builder velocityPort(int velocityPort) {
            this.velocityPort = velocityPort;
            return this;
        }

        public Builder channelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder serverNameColor(String serverNameColor) {
            this.serverNameColor = serverNameColor;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ClusterConfig build() {
            return new ClusterConfig(this);
        }
    }

    public static ClusterConfig defaultConfig() {
        return builder().build();
    }

    public static ClusterConfig velocityConfig(String serverName) {
        return builder()
            .mode(ClusterMode.VELOCITY)
            .serverName(serverName)
            .build();
    }

    public static ClusterConfig velocityConfig(String serverName, String channelName) {
        return builder()
            .mode(ClusterMode.VELOCITY)
            .serverName(serverName)
            .channelName(channelName)
            .build();
    }
}
