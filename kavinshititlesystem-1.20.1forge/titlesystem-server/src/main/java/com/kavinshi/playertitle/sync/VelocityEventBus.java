package com.kavinshi.playertitle.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于Velocity插件消息的集群事件总线实现。
 * 
 * 通过Velocity的Plugin Messaging API实现跨服事件通信。
 * 需要Velocity代理服务器和相应的Velocity插件配合使用。
 * 
 * 工作原理：
 * 1. 使用Forge的网络系统注册插件消息通道
 * 2. 通过玩家连接发送消息到Velocity代理
 * 3. Velocity插件接收消息并转发到其他服务器
 * 4. 其他服务器的Velocity插件接收消息并发布到本地事件总线
 * 
 * 配置要求：
 * - velocity.channel: 插件消息通道名称（默认：playertitle:sync）
 * - velocity.enabled: 是否启用Velocity模式
 */
public class VelocityEventBus implements ClusterEventBus {
    
    // 监听器映射
    private final Map<ClusterEventType, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Velocity配置参数
    private final String channelName;
    private final String serverName;
    private final String host;
    private final int port;
    
    // TCP连接组件
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Thread receiverThread;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建Velocity事件总线实例。
     * 
     * @param channelName 插件消息通道名称
     * @param serverName 当前服务器名称（用于消息路由）
     * @param host Velocity代理主机地址
     * @param port Velocity代理端口
     */
    public VelocityEventBus(String channelName, String serverName, String host, int port) {
        this.channelName = channelName != null ? channelName : "playertitle:sync";
        this.serverName = serverName != null ? serverName : "unknown";
        this.host = host != null ? host : "localhost";
        this.port = port > 0 ? port : 25577; // Velocity默认端口
    }
    
    /**
     * 创建使用默认配置的Velocity事件总线实例（localhost:25577）。
     */
    public VelocityEventBus(String channelName, String serverName) {
        this(channelName, serverName, "localhost", 25577);
    }
    
    /**
     * 创建使用默认配置的Velocity事件总线实例。
     */
    public VelocityEventBus() {
        this(null, null);
    }
    
    @Override
    public void publish(ClusterSyncEvent event) throws EventBusException {
        if (!running.get()) {
            throw new EventBusException("VelocityEventBus is not running");
        }
        
        try {
            // 序列化事件为JSON
            String serializedEvent = objectMapper.writeValueAsString(event);
            
            // 创建Velocity消息包
            VelocityMessage message = new VelocityMessage(
                channelName,
                serverName,
                event.getEventType().name(),
                serializedEvent
            );
            
            // 发送消息
            sendVelocityMessage(objectMapper.writeValueAsString(message));
            
            System.out.println("[VelocityEventBus] Published event to Velocity: " + event.getEventType());
            
        } catch (JsonProcessingException e) {
            throw new EventBusException("Failed to serialize event for Velocity", e);
        } catch (Exception e) {
            throw new EventBusException("Failed to publish event to Velocity", e);
        }
    }
    
    @Override
    public void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (eventType == null) {
            throw new EventBusException("eventType cannot be null");
        }
        if (listener == null) {
            throw new EventBusException("listener cannot be null");
        }
        
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
        System.out.println("[VelocityEventBus] Subscribed listener to event type: " + eventType);
    }
    
    @Override
    public void subscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("listener cannot be null");
        }
        
        for (ClusterEventType eventType : ClusterEventType.values()) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
        }
        System.out.println("[VelocityEventBus] Subscribed listener to all event types");
    }
    
    @Override
    public void unsubscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (eventType == null || listener == null) {
            return;
        }
        
        Set<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }
    
    @Override
    public void unsubscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            return;
        }
        
        for (Map.Entry<ClusterEventType, Set<EventListener>> entry : listeners.entrySet()) {
            entry.getValue().remove(listener);
            if (entry.getValue().isEmpty()) {
                listeners.remove(entry.getKey());
            }
        }
    }
    
    @Override
    public void start() throws EventBusException {
        if (running.compareAndSet(false, true)) {
            try {
                // 建立TCP连接到Velocity代理
                connectToVelocity();
                
                // 启动消息接收线程
                startReceiverThread();
                
                System.out.println("[VelocityEventBus] Started - Connected to " + host + ":" + port + ", Channel: " + channelName);
                
            } catch (Exception e) {
                running.set(false);
                throw new EventBusException("Failed to start VelocityEventBus - Unable to connect to Velocity proxy at " + host + ":" + port, e);
            }
        }
    }
    
    @Override
    public void stop() throws EventBusException {
        if (running.compareAndSet(true, false)) {
            try {
                // 停止接收线程
                if (receiverThread != null && receiverThread.isAlive()) {
                    receiverThread.interrupt();
                    try {
                        receiverThread.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 关闭连接
                closeConnection();
                
                listeners.clear();
                System.out.println("[VelocityEventBus] Stopped - Disconnected from Velocity proxy");
                
            } catch (Exception e) {
                throw new EventBusException("Failed to stop VelocityEventBus", e);
            }
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public String getImplementationName() {
        return "Velocity";
    }
    
    /**
     * 发送消息到Velocity代理。
     * 
     * @param message 消息内容
     */
    private void sendVelocityMessage(String message) {
        if (writer == null) {
            System.err.println("[VelocityEventBus] Cannot send message - not connected to Velocity proxy");
            return;
        }
        
        try {
            synchronized (writer) {
                writer.write(message);
                writer.newLine();
                writer.flush();
            }
            System.out.println("[VelocityEventBus] Sent message to Velocity: " + message.length() + " chars");
        } catch (IOException e) {
            System.err.println("[VelocityEventBus] Failed to send message to Velocity: " + e.getMessage());
            // 尝试重新连接
            tryReconnect();
        }
    }
    
    /**
     * 处理从Velocity接收到的消息。
     * 
     * @param message 消息内容
     */
    private void handleVelocityMessage(String message) {
        try {
            // 解析Velocity消息
            VelocityMessage velocityMessage = objectMapper.readValue(message, VelocityMessage.class);
            
            // 忽略来自当前服务器的消息（防止循环）
            if (serverName.equals(velocityMessage.getSourceServer())) {
                return;
            }
            
            // 反序列化事件
            ClusterSyncEvent event = objectMapper.readValue(velocityMessage.getEventData(), ClusterSyncEvent.class);
            
            // 分发事件给所有监听器
            ClusterEventType eventType = event.getEventType();
            Set<EventListener> eventListeners = listeners.get(eventType);
            if (eventListeners != null) {
                for (EventListener listener : eventListeners) {
                    listener.onEvent(event);
                }
            }
            
            System.out.println("[VelocityEventBus] Processed event from server " + velocityMessage.getSourceServer() + ": " + eventType);
            
        } catch (JsonProcessingException e) {
            System.err.println("[VelocityEventBus] Failed to parse Velocity message: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[VelocityEventBus] Failed to handle Velocity message: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前配置的通道名称。
     * 
     * @return 通道名称
     */
    public String getChannelName() {
        return channelName;
    }
    
    /**
     * 获取当前服务器名称。
     * 
     * @return 服务器名称
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * 建立TCP连接到Velocity代理。
     */
    private void connectToVelocity() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(5000); // 5秒超时
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        System.out.println("[VelocityEventBus] Connected to Velocity proxy at " + host + ":" + port);
    }
    
    /**
     * 启动消息接收线程。
     */
    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    String line = reader.readLine();
                    if (line == null) {
                        // 连接已关闭
                        System.err.println("[VelocityEventBus] Connection to Velocity proxy lost");
                        tryReconnect();
                        break;
                    }
                    handleVelocityMessage(line);
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[VelocityEventBus] Error reading from Velocity proxy: " + e.getMessage());
                    tryReconnect();
                }
            }
        }, "VelocityEventBus-Receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }
    
    /**
     * 关闭连接。
     */
    private void closeConnection() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            // 忽略
        }
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            // 忽略
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // 忽略
        }
        writer = null;
        reader = null;
        socket = null;
    }
    
    /**
     * 尝试重新连接到Velocity代理。
     */
    private void tryReconnect() {
        if (!running.get()) {
            return;
        }
        
        System.out.println("[VelocityEventBus] Attempting to reconnect to Velocity proxy...");
        closeConnection();
        
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(2000 * attempt); // 指数退避
                connectToVelocity();
                System.out.println("[VelocityEventBus] Reconnected to Velocity proxy");
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                System.err.println("[VelocityEventBus] Reconnection attempt " + attempt + " failed: " + e.getMessage());
            }
        }
        
        System.err.println("[VelocityEventBus] Failed to reconnect to Velocity proxy after " + maxAttempts + " attempts");
    }
    
    /**
     * Velocity消息数据结构。
     */
    public static class VelocityMessage {
        private final String channel;
        private final String sourceServer;
        private final String eventType;
        private final String eventData;
        
        public VelocityMessage(String channel, String sourceServer, String eventType, String eventData) {
            this.channel = channel;
            this.sourceServer = sourceServer;
            this.eventType = eventType;
            this.eventData = eventData;
        }
        
        public String getChannel() {
            return channel;
        }
        
        public String getSourceServer() {
            return sourceServer;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public String getEventData() {
            return eventData;
        }
    }
}