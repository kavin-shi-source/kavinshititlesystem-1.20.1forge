package com.kavinshi.playertitle.sync;

/**
 * 集群通信模式枚举。
 */
public enum ClusterMode {
    /**
     * 本地模式 - 仅在当前服务器内通信。
     * 适用于单服务器环境或测试。
     */
    LOCAL,
    
    /**
     * Redis模式 - 通过Redis Pub/Sub进行跨服务器通信。
     * 适用于多服务器环境，需要Redis服务器。
     */
    REDIS,
    
    /**
     * Velocity模式 - 通过Velocity代理进行通信。
     * 适用于使用Velocity代理的多服务器环境。
     */
    VELOCITY;
    
    /**
     * 从字符串解析集群模式（不区分大小写）。
     * 
     * @param value 模式字符串
     * @return 集群模式，如果无法解析则返回LOCAL
     */
    public static ClusterMode fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LOCAL;
        }
        
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOCAL; // 默认值
        }
    }
    
    /**
     * 检查是否为本地模式。
     */
    public boolean isLocal() {
        return this == LOCAL;
    }
    
    /**
     * 检查是否为Redis模式。
     */
    public boolean isRedis() {
        return this == REDIS;
    }
    
    /**
     * 检查是否为Velocity模式。
     */
    public boolean isVelocity() {
        return this == VELOCITY;
    }
}