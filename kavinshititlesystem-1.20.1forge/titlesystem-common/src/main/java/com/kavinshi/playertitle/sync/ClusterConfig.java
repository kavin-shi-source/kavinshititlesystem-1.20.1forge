package com.kavinshi.playertitle.sync;

/**
 * 集群通信配置类。
 */
public class ClusterConfig {
    private final ClusterMode mode;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final String velocityHost;
    private final int velocityPort;
    private final String channelName;
    private final String serverName;
    private final boolean enabled;
    
    private ClusterConfig(Builder builder) {
        this.mode = builder.mode;
        this.redisHost = builder.redisHost;
        this.redisPort = builder.redisPort;
        this.redisPassword = builder.redisPassword;
        this.velocityHost = builder.velocityHost;
        this.velocityPort = builder.velocityPort;
        this.channelName = builder.channelName;
        this.serverName = builder.serverName;
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
    
    public String getRedisHost() {
        return redisHost;
    }
    
    public int getRedisPort() {
        return redisPort;
    }
    
    public String getRedisPassword() {
        return redisPassword;
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
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 构建器类。
     */
    public static class Builder {
        private ClusterMode mode = ClusterMode.LOCAL;
        private String redisHost = "localhost";
        private int redisPort = 6379;
        private String redisPassword = null;
        private String velocityHost = "localhost";
        private int velocityPort = 25577;
        private String channelName = "playertitle:sync";
        private String serverName = "unknown";
        private boolean enabled = true;
        
        public Builder() {}
        
        public Builder(ClusterConfig config) {
            this.mode = config.mode;
            this.redisHost = config.redisHost;
            this.redisPort = config.redisPort;
            this.redisPassword = config.redisPassword;
            this.velocityHost = config.velocityHost;
            this.velocityPort = config.velocityPort;
            this.channelName = config.channelName;
            this.serverName = config.serverName;
            this.enabled = config.enabled;
        }
        
        public Builder mode(ClusterMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder redisHost(String redisHost) {
            this.redisHost = redisHost;
            return this;
        }
        
        public Builder redisPort(int redisPort) {
            this.redisPort = redisPort;
            return this;
        }
        
        public Builder redisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
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
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public ClusterConfig build() {
            return new ClusterConfig(this);
        }
    }
    
    /**
     * 创建默认配置。
     */
    public static ClusterConfig defaultConfig() {
        return builder().build();
    }
    
    /**
     * 创建Redis配置。
     */
    public static ClusterConfig redisConfig(String host, int port) {
        return builder()
            .mode(ClusterMode.REDIS)
            .redisHost(host)
            .redisPort(port)
            .build();
    }
    
    /**
     * 创建Redis配置（带密码）。
     */
    public static ClusterConfig redisConfig(String host, int port, String password) {
        return builder()
            .mode(ClusterMode.REDIS)
            .redisHost(host)
            .redisPort(port)
            .redisPassword(password)
            .build();
    }
    
    /**
     * 创建Velocity配置。
     */
    public static ClusterConfig velocityConfig(String host, int port, String serverName) {
        return builder()
            .mode(ClusterMode.VELOCITY)
            .velocityHost(host)
            .velocityPort(port)
            .serverName(serverName)
            .build();
    }
}