package com.kavinshi.playertitle.sync;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.kavinshi.playertitle.sync.TitleAssignedEvent;
import com.kavinshi.playertitle.sync.TitleRemovedEvent;
import com.kavinshi.playertitle.sync.TitleUpdatedEvent;
import com.kavinshi.playertitle.sync.TitleProgressUpdatedEvent;
import com.kavinshi.playertitle.sync.TitleEquipStateChangedEvent;

/**
 * Redis事件总线实现，用于跨服事件同步。
 * 使用Redis Pub/Sub进行事件分发，支持多个服务器之间的实时同步。
 * 
 * 注意：需要添加Redis客户端依赖（如Jedis或Lettuce）到build.gradle：
 * implementation 'redis.clients:jedis:5.0.0'
 */
public class RedisEventBus implements ClusterEventBus {
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final String channelPrefix;
    
    // Redis客户端实例
    private JedisPool jedisPool;
    private JedisPubSub jedisPubSub;
    private Thread listenerThread;
    
    private final Map<ClusterEventType, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建Redis事件总线。
     *
     * @param redisHost Redis主机地址
     * @param redisPort Redis端口
     * @param redisPassword Redis密码（可为null）
     * @param channelPrefix 通道前缀，用于区分不同环境
     */
    public RedisEventBus(String redisHost, int redisPort, String redisPassword, String channelPrefix) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
        this.channelPrefix = channelPrefix == null ? "titlesystem" : channelPrefix;
    }
    
    /**
     * 创建Redis事件总线（使用默认前缀）。
     */
    public RedisEventBus(String redisHost, int redisPort, String redisPassword) {
        this(redisHost, redisPort, redisPassword, "titlesystem");
    }
    
    /**
     * 创建Redis事件总线（使用默认端口和前缀）。
     */
    public RedisEventBus(String redisHost) {
        this(redisHost, 6379, null, "titlesystem");
    }
    
    @Override
    public void publish(ClusterSyncEvent event) throws EventBusException {
        if (!running.get()) {
            throw new EventBusException("Redis event bus is not running");
        }
        
        if (event == null) {
            throw new EventBusException("Event cannot be null");
        }
        
        event.validate();
        
        try {
            // 实现Redis发布逻辑
            String channel = getChannelName(event.getEventType());
            String message = serializeEvent(event);
            
            System.out.println("[RedisEventBus] Publishing event to channel " + channel + ": " + event);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
            }
            
        } catch (Exception e) {
            throw new EventBusException("Failed to publish event: " + event.getEventId(), e);
        }
    }
    
    @Override
    public void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        if (!running.get()) {
            throw new EventBusException("Redis event bus is not running");
        }
        
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
        System.out.println("[RedisEventBus] Subscribed listener to event type: " + eventType);
        
        // TODO: 如果这是该事件类型的第一个监听器，可能需要订阅Redis频道
    }
    
    @Override
    public void subscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        listeners.computeIfAbsent(null, k -> new CopyOnWriteArraySet<>()).add(listener);
        System.out.println("[RedisEventBus] Subscribed listener to all event types");
    }
    
    @Override
    public void unsubscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        Set<EventListener> typeListeners = listeners.get(eventType);
        if (typeListeners != null) {
            typeListeners.remove(listener);
            if (typeListeners.isEmpty()) {
                listeners.remove(eventType);
                // TODO: 如果这是该事件类型的最后一个监听器，可能需要取消订阅Redis频道
            }
            System.out.println("[RedisEventBus] Unsubscribed listener from event type: " + eventType);
        }
    }
    
    @Override
    public void unsubscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        // 从所有事件类型中移除
        for (Map.Entry<ClusterEventType, Set<EventListener>> entry : listeners.entrySet()) {
            entry.getValue().remove(listener);
            if (entry.getValue().isEmpty()) {
                listeners.remove(entry.getKey());
            }
        }
        System.out.println("[RedisEventBus] Unsubscribed listener from all event types");
    }
    
    @Override
    public void start() throws EventBusException {
        if (running.compareAndSet(false, true)) {
            try {
                // 初始化Jedis连接池
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(10);
                poolConfig.setMaxIdle(5);
                poolConfig.setMinIdle(1);
                poolConfig.setTestOnBorrow(true);
                
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
                } else {
                    jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000);
                }
                
                // 创建JedisPubSub实例
                jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleRedisMessage(channel, message);
                    }
                    
                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        System.out.println("[RedisEventBus] Subscribed to channel: " + channel);
                    }
                    
                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        System.out.println("[RedisEventBus] Unsubscribed from channel: " + channel);
                    }
                };
                
                // 启动Redis订阅线程
                listenerThread = new Thread(() -> {
                    try (Jedis jedis = jedisPool.getResource()) {
                        jedis.subscribe(jedisPubSub, getAllChannels());
                    } catch (Exception e) {
                        if (running.get()) {
                            System.err.println("[RedisEventBus] Redis subscription thread error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                listenerThread.setDaemon(true);
                listenerThread.setName("RedisEventBus-Listener");
                listenerThread.start();
                
                System.out.println("[RedisEventBus] Started successfully - Redis host: " + redisHost + ":" + redisPort);
                
            } catch (Exception e) {
                running.set(false);
                throw new EventBusException("Failed to start Redis event bus", e);
            }
        }
    }
    
    @Override
    public void stop() throws EventBusException {
        if (running.compareAndSet(true, false)) {
            try {
                // 停止Redis订阅线程
                if (jedisPubSub != null) {
                    jedisPubSub.unsubscribe();
                }
                if (listenerThread != null && listenerThread.isAlive()) {
                    listenerThread.interrupt();
                    try {
                        listenerThread.join(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("[RedisEventBus] Interrupted while waiting for listener thread to stop");
                    }
                }
                if (jedisPool != null) {
                    jedisPool.close();
                }
                
                listeners.clear();
                System.out.println("[RedisEventBus] Stopped");
                
            } catch (Exception e) {
                throw new EventBusException("Failed to stop Redis event bus", e);
            }
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public String getImplementationName() {
        return "Redis";
    }
    
    /**
     * 获取Redis频道名称。
     *
     * @param eventType 事件类型
     * @return 完整的Redis频道名称
     */
    private String getChannelName(ClusterEventType eventType) {
        return channelPrefix + ":events:" + eventType.name().toLowerCase();
    }
    
    /**
     * 获取所有需要订阅的Redis频道。
     *
     * @return 频道数组
     */
    private String[] getAllChannels() {
        return listeners.keySet().stream()
            .filter(type -> type != null)
            .map(this::getChannelName)
            .toArray(String[]::new);
    }
    
    /**
     * 序列化事件为JSON字符串。
     *
     * @param event 事件
     * @return JSON字符串
     */
    private String serializeEvent(ClusterSyncEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            System.err.println("[RedisEventBus] Failed to serialize event: " + e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
    
    /**
     * 反序列化JSON字符串为事件。
     *
     * @param json JSON字符串
     * @return 事件对象
     */
    private ClusterSyncEvent deserializeEvent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String eventTypeStr = root.get("eventType").asText();
            ClusterEventType eventType = ClusterEventType.valueOf(eventTypeStr);
            
            Class<? extends ClusterSyncEvent> targetClass = null;
            switch (eventType) {
                case TITLE_ASSIGNED:
                    targetClass = TitleAssignedEvent.class;
                    break;
                case TITLE_REMOVED:
                    targetClass = TitleRemovedEvent.class;
                    break;
                case TITLE_UPDATED:
                    targetClass = TitleUpdatedEvent.class;
                    break;
                case TITLE_PROGRESS_UPDATED:
                    targetClass = TitleProgressUpdatedEvent.class;
                    break;
                case TITLE_EQUIP_STATE_CHANGED:
                    targetClass = TitleEquipStateChangedEvent.class;
                    break;
                case SERVER_ANNOUNCEMENT:
                    // 服务器公告事件暂无具体实现，返回null
                    System.err.println("[RedisEventBus] SERVER_ANNOUNCEMENT event type not implemented");
                    return null;
                default:
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
            }
            
            return objectMapper.readValue(json, targetClass);
        } catch (Exception e) {
            System.err.println("[RedisEventBus] Failed to deserialize event from JSON: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理从Redis接收到的消息。
     *
     * @param channel Redis频道
     * @param message 消息内容
     */
    private void handleRedisMessage(String channel, String message) {
        try {
            ClusterSyncEvent event = deserializeEvent(message);
            if (event == null) {
                System.err.println("[RedisEventBus] Failed to deserialize event from message: " + message);
                return;
            }
            
            // 分发给监听器
            ClusterEventType eventType = event.getEventType();
            Set<EventListener> typeListeners = listeners.get(eventType);
            if (typeListeners != null) {
                for (EventListener listener : typeListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        System.err.println("[RedisEventBus] Error in listener for event " + event.getEventId() + ": " + e.getMessage());
                    }
                }
            }
            
            // 分发给全局监听器
            Set<EventListener> allListeners = listeners.get(null);
            if (allListeners != null) {
                for (EventListener listener : allListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        System.err.println("[RedisEventBus] Error in global listener for event " + event.getEventId() + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[RedisEventBus] Error handling Redis message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取Redis主机地址。
     *
     * @return Redis主机
     */
    public String getRedisHost() {
        return redisHost;
    }
    
    /**
     * 获取Redis端口。
     *
     * @return Redis端口
     */
    public int getRedisPort() {
        return redisPort;
    }
    
    /**
     * 获取通道前缀。
     *
     * @return 通道前缀
     */
    public String getChannelPrefix() {
        return channelPrefix;
    }
}