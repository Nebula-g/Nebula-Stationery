package com.nebula.stationery.mq;

/**
 * 业务侧实现这个接口即可订阅一类消息；同一个类型只允许一个处理器。
 */
public interface MessageHandler {

    String supportedType();

    void handle(Message message);
}
