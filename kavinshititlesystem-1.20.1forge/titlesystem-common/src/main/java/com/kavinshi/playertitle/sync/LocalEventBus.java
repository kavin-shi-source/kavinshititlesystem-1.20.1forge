package com.kavinshi.playertitle.sync;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地事件总线实现，用于在单个JVM内处理集群同步事件。
 * 提供事件发布、订阅、取消订阅功能，支持按事件类型订阅和全局订阅。
 */
public class LocalEventBus implements ClusterEventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEventBus.class);
    private final Map<ClusterEventType, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final Set<EventListener> globalListeners = new CopyOnWriteArraySet<>();
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

        ClusterEventType eventType = event.getEventType();
        if (eventType != null) {
            Set<EventListener> typeListeners = listeners.get(eventType);
            if (typeListeners != null) {
                for (EventListener listener : typeListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.error("Error in event listener for event {}: {}", event.getEventId(), e.getMessage());
                    }
                }
            }
        }

        for (EventListener listener : globalListeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error in global event listener for event {}: {}", event.getEventId(), e.getMessage());
            }
        }
    }

    @Override
    public void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        if (eventType == null) {
            globalListeners.add(listener);
            return;
        }
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(listener);
    }

    @Override
    public void subscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        globalListeners.add(listener);
    }

    @Override
    public void unsubscribe(ClusterEventType eventType, EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        if (eventType == null) {
            globalListeners.remove(listener);
            return;
        }
        Set<EventListener> typeListeners = listeners.get(eventType);
        if (typeListeners != null) {
            typeListeners.remove(listener);
            if (typeListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    @Override
    public void unsubscribeAll(EventListener listener) throws EventBusException {
        if (listener == null) {
            throw new EventBusException("Listener cannot be null");
        }
        globalListeners.remove(listener);
        for (Map.Entry<ClusterEventType, Set<EventListener>> entry : listeners.entrySet()) {
            entry.getValue().remove(listener);
            if (entry.getValue().isEmpty()) {
                listeners.remove(entry.getKey());
            }
        }
    }

    @Override
    public void start() throws EventBusException {
        running.compareAndSet(false, true);
    }

    @Override
    public void stop() throws EventBusException {
        if (running.compareAndSet(true, false)) {
            listeners.clear();
            globalListeners.clear();
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

    public int getListenerCount(ClusterEventType eventType) {
        if (eventType == null) return globalListeners.size();
        Set<EventListener> typeListeners = listeners.get(eventType);
        return typeListeners == null ? 0 : typeListeners.size();
    }

    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(Set::size).sum() + globalListeners.size();
    }

    public void clearListeners() {
        listeners.clear();
        globalListeners.clear();
    }
}
