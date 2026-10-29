package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.OrderRequest;
import com.nebula.stationery.entity.CartItem;
import com.nebula.stationery.entity.FlashSaleOrder;
import com.nebula.stationery.entity.Order;
import com.nebula.stationery.entity.OrderItem;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.entity.ProductSku;
import com.nebula.stationery.mapper.CartItemMapper;
import com.nebula.stationery.mapper.FlashSaleOrderMapper;
import com.nebula.stationery.mapper.OrderItemMapper;
import com.nebula.stationery.mapper.OrderMapper;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.ProductSkuMapper;
import com.nebula.stationery.vo.OrderItemVO;
import com.nebula.stationery.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductService productService;
    private final FlashSaleOrderMapper flashSaleOrderMapper;
    private final LogisticsService logisticsService;
    // 消息队列持有所有 Handler，而 Handler 又依赖本类，直接用 ObjectProvider 延迟取，避免循环依赖
    private final org.springframework.beans.factory.ObjectProvider<com.nebula.stationery.mq.MessageQueueService> messageQueue;

    /** 未支付订单多久后自动取消，演示时可调小 */
    @org.springframework.beans.factory.annotation.Value("${stationery.order.pay-timeout-minutes:720}")
    private long payTimeoutMinutes;

    @Transactional
    public OrderVO createOrder(Long userId, OrderRequest request) {
        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .in(CartItem::getId, request.getCartItemIds())
        );

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车项不存在");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException("商品" + cartItem.getProductId() + "已下架，无法结算");
            }

            BigDecimal itemPrice;
            if (cartItem.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(cartItem.getSkuId());
                if (sku == null || sku.getStatus() == 0) {
                    throw new BusinessException("商品" + product.getName() + "的规格已失效");
                }
                if (sku.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("商品" + product.getName() + "（" + sku.getSpecText() + "）库存不足");
                }
                itemPrice = sku.getPrice();
            } else {
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("商品" + product.getName() + "库存不足");
                }
                itemPrice = product.getPrice();
            }
            totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        orderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            BigDecimal itemPrice;
            String specText = null;

            if (cartItem.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(cartItem.getSkuId());
                itemPrice = sku.getPrice();
                specText = sku.getSpecText();
                sku.setStock(sku.getStock() - cartItem.getQuantity());
                productSkuMapper.updateById(sku);
            } else {
                itemPrice = product.getPrice();
                product.setStock(product.getStock() - cartItem.getQuantity());
                productMapper.updateById(product);
            }
            productService.evictProductCache(cartItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setSkuId(cartItem.getSkuId());
            orderItem.setSpecText(specText);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(itemPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .in(CartItem::getId, request.getCartItemIds())
        );

        // 延迟消息：到点仍是待支付就由系统取消（和定时兜底任务形成双保险）
        messageQueue.getObject().publishDelayed(
                com.nebula.stationery.mq.MessageQueueService.TOPIC_ORDER,
                "order.pay.timeout",
                java.util.Map.of("orderId", String.valueOf(order.getId())),
                java.time.Duration.ofMinutes(payTimeoutMinutes)
        );

        return getOrderDetail(order.getId(), userId);
    }

    public Page<OrderVO> getOrderList(Long userId, Integer status, Integer page, Integer size) {
        List<OrderVO> allOrders = new ArrayList<>();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        List<Order> normalOrders = orderMapper.selectList(wrapper);
        for (Order order : normalOrders) {
            OrderVO vo = convertToVO(order);
            vo.setOrderType("normal");
            allOrders.add(vo);
        }

        LambdaQueryWrapper<FlashSaleOrder> fsWrapper = new LambdaQueryWrapper<>();
        fsWrapper.eq(FlashSaleOrder::getUserId, userId);
        if (status != null) {
            if (status == 4) {
                fsWrapper.eq(FlashSaleOrder::getStatus, 2);
            } else {
                fsWrapper.eq(FlashSaleOrder::getStatus, status);
            }
        }
        fsWrapper.orderByDesc(FlashSaleOrder::getCreatedAt);
        List<FlashSaleOrder> fsOrders = flashSaleOrderMapper.selectList(fsWrapper);
        for (FlashSaleOrder fsOrder : fsOrders) {
            allOrders.add(convertFlashSaleToVO(fsOrder));
        }

        allOrders.sort(Comparator.comparing(OrderVO::getCreatedAt).reversed());

        int total = allOrders.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<OrderVO> pageList = (fromIndex < total) ? allOrders.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<OrderVO> voPage = new Page<>(page, size, total);
        voPage.setRecords(pageList);
        return voPage;
    }

    public OrderVO getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }

        OrderVO vo = convertToVO(order);
        vo.setLogistics(logisticsService.getView(orderId));
        return vo;
    }

    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付订单可以付款");
        }

        order.setStatus(1); // 已支付
        orderMapper.updateById(order);

        // 支付成功后累计销量，供销量统计使用
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSoldCount(product.getSoldCount() + item.getQuantity());
                productMapper.updateById(product);
                productService.evictProductCache(item.getProductId());
            }
        }
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付订单可以取消");
        }

        restoreOrderStock(orderId);

        order.setStatus(4); // 已取消
        orderMapper.updateById(order);
    }

    /**
     * 消息队列消费端使用：超时未支付订单由系统取消。
     * @return false 表示订单已支付或已取消，无需处理
     */
    @Transactional
    public boolean cancelExpiredOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return false;
        }
        restoreOrderStock(orderId);
        order.setStatus(4);
        orderMapper.updateById(order);
        return true;
    }

    private void restoreOrderStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : orderItems) {
            if (item.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                if (sku != null) {
                    sku.setStock(sku.getStock() + item.getQuantity());
                    productSkuMapper.updateById(sku);
                }
            } else {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                }
            }
            productService.evictProductCache(item.getProductId());
        }
    }

    @Transactional
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException("只有已支付或已发货订单可以确认收货");
        }

        order.setStatus(3); // 已完成
        orderMapper.updateById(order);
    }

    // 后台管理方法
    public Page<OrderVO> getAllOrders(Integer status, Integer page, Integer size) {
        Page<Order> orderPage = new Page<>(page, size);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        orderMapper.selectPage(orderPage, wrapper);

        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    public OrderVO getAdminOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderVO vo = convertToVO(order);
        vo.setLogistics(logisticsService.getView(orderId));
        return vo;
    }

    @Transactional
    public void shipOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("只有已支付订单可以标记发货");
        }

        logisticsService.ship(orderId, "SF", null, null);
        order.setStatus(2); // 已发货
        orderMapper.updateById(order);
    }

    /** 后台发货：可填快递公司和运单号，留空自动生成 */
    @Transactional
    public OrderVO shipOrder(Long orderId, com.nebula.stationery.dto.ShipRequest request) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("只有已支付订单可以标记发货");
        }

        String companyCode = request == null ? null : request.getCompanyCode();
        if (companyCode != null && !companyCode.isBlank()) {
            logisticsService.companyName(companyCode);
        }
        logisticsService.ship(
                orderId,
                companyCode,
                request == null ? null : request.getTrackingNo(),
                null
        );
        order.setStatus(2);
        orderMapper.updateById(order);
        return getAdminOrderDetail(orderId);
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType("normal");
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setCreatedAt(order.getCreatedAt());

        // 查询订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setSkuId(item.getSkuId());
            itemVO.setSpecText(item.getSpecText());
            itemVO.setProductName(item.getProductName());
            itemVO.setUnitPrice(item.getUnitPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    private OrderVO convertFlashSaleToVO(FlashSaleOrder fsOrder) {
        OrderVO vo = new OrderVO();
        vo.setId(fsOrder.getId());
        vo.setOrderNo("FS" + fsOrder.getId());
        vo.setOrderType("flash_sale");
        vo.setTotalAmount(fsOrder.getTotalAmount());
        int mappedStatus = (fsOrder.getStatus() == 2) ? 4 : fsOrder.getStatus();
        vo.setStatus(mappedStatus);
        vo.setStatusText(getStatusText(mappedStatus));
        vo.setCreatedAt(fsOrder.getCreatedAt());

        Product product = productMapper.selectById(fsOrder.getProductId());
        OrderItemVO itemVO = new OrderItemVO();
        itemVO.setId(fsOrder.getId());
        itemVO.setProductId(fsOrder.getProductId());
        itemVO.setProductName(product != null ? product.getName() : "未知商品");
        itemVO.setUnitPrice(fsOrder.getTotalAmount().divide(BigDecimal.valueOf(fsOrder.getQuantity()), RoundingMode.HALF_UP));
        itemVO.setQuantity(fsOrder.getQuantity());
        itemVO.setSubtotal(fsOrder.getTotalAmount());
        vo.setItems(List.of(itemVO));

        return vo;
    }
}
