package com.kavinshi.playertitle.sync;

/**
 * 集群事件总线接口，定义跨服事件发布和订阅的契约。
 * 实现可以是Redis Pub/Sub、Kafka、RabbitMQ或本地事件总线。
 */
public interface ClusterEventBus {
    
    /**
     * 发布事件到集群。
     *
     * @param event 要发布的事件
     * @throws EventBusException 如果发布失败
     */
    void publish(ClusterSyncEvent event) throws EventBusException;
    
    /**
     * 订阅指定类型的事件。
     *
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @throws EventBusException 如果订阅失败
     */
    void subscribe(ClusterEventType eventType, EventListener listener) throws EventBusException;
    
    /**
     * 订阅所有类型的事件。
     *
     * @param listener 事件监听器
     * @throws EventBusException 如果订阅失败
     */
    void subscribeAll(EventListener listener) throws EventBusException;
    
    /**
     * 取消订阅指定类型的事件。
     *
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @throws EventBusException 如果取消订阅失败
     */
    void unsubscribe(ClusterEventType eventType, EventListener listener) throws EventBusException;
    
    /**
     * 取消订阅所有事件。
     *
     * @param listener 事件监听器
     * @throws EventBusException 如果取消订阅失败
     */
    void unsubscribeAll(EventListener listener) throws EventBusException;
    
    /**
     * 启动事件总线。
     * 初始化连接、启动监听线程等。
     *
     * @throws EventBusException 如果启动失败
     */
    void start() throws EventBusException;
    
    /**
     * 停止事件总线。
     * 关闭连接、停止监听线程等。
     *
     * @throws EventBusException 如果停止失败
     */
    void stop() throws EventBusException;
    
    /**
     * 检查事件总线是否正在运行。
     *
     * @return 如果正在运行返回true
     */
    boolean isRunning();
    
    /**
     * 获取事件总线的实现名称。
     *
     * @return 实现名称（如"Redis"、"Local"等）
     */
    String getImplementationName();
    
    /**
     * 事件监听器接口。
     */
    @FunctionalInterface
    interface EventListener {
        /**
         * 处理接收到的事件。
         *
         * @param event 接收到的事件
         */
        void onEvent(ClusterSyncEvent event);
    }
    
    /**
     * 事件总线异常。
     */
    class EventBusException extends Exception {
        public EventBusException(String message) {
            super(message);
        }
        
        public EventBusException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}