package com.kavinshi.playertitle.sync;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地事件总线实现，用于单服务器环境或测试。
 * 不涉及网络通信，所有事件在JVM内部分发。
 */
public class LocalEventBus implements ClusterEventBus {
    private final Map<ClusterEventType, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @Override
    public void publish(ClusterSyncEvent event) throws EventBusException {
        if (!running.get()) {
            throw new EventBusException("Event bus is not running");
        }
        
        if (event == null) {
            throw new EventBusException("Event cannot be null");
        }
        
        event.validate();
        
        // 分发给特定事件类型的监听器
        Set<EventListener> typeListeners = listeners.get(event.getEventType());
        if (typeListeners != null) {
            for (EventListener listener : typeListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.println("Error in event listener for event " + event.getEventId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // 分发给订阅所有事件的监听器
        Set<EventListener> allListeners = listeners.get(null);
        if (allListeners != null) {
            for (EventListener listener : allListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.println("Error in global event listener for event " + event.getEventId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Published event: " + event);
    }
    
    @Override
    public void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
        System.out.println("Subscribed listener to event type: " + eventType);
    }
    
    @Override
    public void subscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        
        listeners.computeIfAbsent(null, k -> new CopyOnWriteArraySet<>()).add(listener);
        System.out.println("Subscribed listener to all event types");
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
            }
            System.out.println("Unsubscribed listener from event type: " + eventType);
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
        System.out.println("Unsubscribed listener from all event types");
    }
    
    @Override
    public void start() throws EventBusException {
        if (running.compareAndSet(false, true)) {
            System.out.println("Local event bus started");
        }
    }
    
    @Override
    public void stop() throws EventBusException {
        if (running.compareAndSet(true, false)) {
            listeners.clear();
            System.out.println("Local event bus stopped");
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public String getImplementationName() {
        return "Local";
    }
    
    /**
     * 获取指定事件类型的监听器数量。
     *
     * @param eventType 事件类型，null表示所有事件
     * @return 监听器数量
     */
    public int getListenerCount(ClusterEventType eventType) {
        Set<EventListener> typeListeners = listeners.get(eventType);
        return typeListeners == null ? 0 : typeListeners.size();
    }
    
    /**
     * 获取总监听器数量（所有事件类型）。
     *
     * @return 总监听器数量
     */
    public int getTotalListenerCount() {
        return listeners.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    /**
     * 清除所有监听器。
     */
    public void clearListeners() {
        listeners.clear();
    }
}