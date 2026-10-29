package com.nebula.stationery.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.entity.*;
import com.nebula.stationery.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final FlashSaleOrderMapper flashSaleOrderMapper;
    private final FlashSaleMapper flashSaleMapper;
    private final GroupSessionMapper groupSessionMapper;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredNormalOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<Order> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .lt(Order::getCreatedAt, threshold)
        );

        if (expiredOrders.isEmpty()) return;

        for (Order order : expiredOrders) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                }
            }
            order.setStatus(4);
            orderMapper.updateById(order);
        }

        log.info("自动取消 {} 个超时未支付普通订单", expiredOrders.size());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredFlashSaleOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<FlashSaleOrder> expiredOrders = flashSaleOrderMapper.selectList(
                new LambdaQueryWrapper<FlashSaleOrder>()
                        .eq(FlashSaleOrder::getStatus, 0)
                        .lt(FlashSaleOrder::getCreatedAt, threshold)
        );

        if (expiredOrders.isEmpty()) return;

        for (FlashSaleOrder order : expiredOrders) {
            order.setStatus(2);
            flashSaleOrderMapper.updateById(order);

            FlashSale fs = flashSaleMapper.selectById(order.getFlashSaleId());
            if (fs != null) {
                fs.setSoldCount(Math.max(0, fs.getSoldCount() - order.getQuantity()));
                flashSaleMapper.updateById(fs);
            }
        }

        log.info("自动取消 {} 个超时未支付秒杀订单", expiredOrders.size());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredGroupSessions() {
        List<GroupSession> expiredSessions = groupSessionMapper.selectList(
                new LambdaQueryWrapper<GroupSession>()
                        .eq(GroupSession::getStatus, 0)
                        .lt(GroupSession::getExpireTime, LocalDateTime.now())
        );

        if (expiredSessions.isEmpty()) return;

        for (GroupSession session : expiredSessions) {
            session.setStatus(2);
            groupSessionMapper.updateById(session);
        }

        log.info("自动取消 {} 个过期拼团", expiredSessions.size());
    }
}
