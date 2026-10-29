package com.nebula.stationery.mq.handler;

import com.nebula.stationery.mq.Message;
import com.nebula.stationery.mq.MessageHandler;
import com.nebula.stationery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 延迟消息到点后取消仍未支付的订单，和定时兜底任务互为补充。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutHandler implements MessageHandler {

    public static final String TYPE = "order.pay.timeout";

    private final OrderService orderService;

    @Override
    public String supportedType() {
        return TYPE;
    }

    @Override
    public void handle(Message message) {
        long orderId = message.longField("orderId");
        boolean cancelled = orderService.cancelExpiredOrder(orderId);
        if (cancelled) {
            log.info("订单 {} 超时未支付，已自动取消并回补库存", orderId);
        }
    }
}
