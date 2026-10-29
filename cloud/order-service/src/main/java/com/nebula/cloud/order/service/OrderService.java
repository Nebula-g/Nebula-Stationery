package com.nebula.cloud.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.cloud.common.BusinessException;
import com.nebula.cloud.common.PageResult;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.order.dto.CreateOrderRequest;
import com.nebula.cloud.order.entity.Order;
import com.nebula.cloud.order.entity.OrderItem;
import com.nebula.cloud.order.feign.ProductClient;
import com.nebula.cloud.order.feign.dto.ProductSnapshot;
import com.nebula.cloud.order.feign.dto.StockChange;
import com.nebula.cloud.order.mapper.OrderItemMapper;
import com.nebula.cloud.order.mapper.OrderMapper;
import com.nebula.cloud.order.vo.OrderBrief;
import com.nebula.cloud.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;

    @Transactional
    public OrderVO create(Long userId, CreateOrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        List<StockChange> changes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.Item line : request.getItems()) {
            ProductSnapshot product = loadProduct(line.getProductId());
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException("商品「" + product.getName() + "」已下架");
            }
            if (product.getStock() == null || product.getStock() < line.getQuantity()) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(line.getQuantity());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            items.add(item);

            changes.add(new StockChange(product.getId(), line.getQuantity()));
            total = total.add(item.getSubtotal());
        }

        // 跨服务扣库存：商品服务先落库，订单写失败要补一次回滚
        Result<Void> deducted = productClient.deduct(changes);
        if (deducted.getCode() != 200) {
            throw new BusinessException(deducted.getMessage());
        }

        try {
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setTotalAmount(total);
            order.setStatus(0);
            orderMapper.insert(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }

            return toVO(order);
        } catch (RuntimeException e) {
            productClient.restore(changes);
            throw e;
        }
    }

    public PageResult<OrderVO> page(Long userId, int page, int size, Integer status) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getId);
        if (status != null) {
            query.eq(Order::getStatus, status);
        }

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), query);
        List<OrderVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    public OrderVO detail(Long userId, Long orderId) {
        return toVO(requireOwned(userId, orderId));
    }

    @Transactional
    public void pay(Long userId, Long orderId) {
        Order order = requireOwned(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付订单可以付款");
        }
        order.setStatus(1);
        orderMapper.updateById(order);
    }

    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = requireOwned(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付订单可以取消");
        }
        restoreStock(orderId);
        order.setStatus(4);
        orderMapper.updateById(order);
    }

    @Transactional
    public void confirm(Long userId, Long orderId) {
        Order order = requireOwned(userId, orderId);
        if (order.getStatus() != 2) {
            throw new BusinessException("只有已发货订单可以确认收货");
        }
        order.setStatus(3);
        orderMapper.updateById(order);
    }

    public OrderBrief brief(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderBrief brief = new OrderBrief();
        brief.setId(order.getId());
        brief.setUserId(order.getUserId());
        brief.setStatus(order.getStatus());
        return brief;
    }

    private Order requireOwned(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        List<StockChange> changes = items.stream()
                .map(item -> new StockChange(item.getProductId(), item.getQuantity()))
                .toList();
        try {
            productClient.restore(changes);
        } catch (Exception e) {
            // 商品服务暂时不可用时先记日志，避免用户卡在「取消不了」
            log.error("回滚库存失败，订单 {} 需要人工处理", orderId, e);
        }
    }

    private ProductSnapshot loadProduct(Long productId) {
        Result<ProductSnapshot> result = productClient.detail(productId);
        if (result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        return result.getData();
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreatedAt(order.getCreatedAt());
        vo.setItems(orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())));
        return vo;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String generateOrderNo() {
        return "NB" + LocalDateTime.now().format(NO_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
