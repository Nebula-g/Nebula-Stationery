package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.entity.FlashSale;
import com.nebula.stationery.entity.FlashSaleOrder;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.mapper.FlashSaleMapper;
import com.nebula.stationery.mapper.FlashSaleOrderMapper;
import com.nebula.stationery.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleOrderMapper flashSaleOrderMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;

    public List<Map<String, Object>> listFlashSales() {
        List<FlashSale> sales = flashSaleMapper.selectList(
                new LambdaQueryWrapper<FlashSale>().eq(FlashSale::getStatus, 1)
        );
        return sales.stream().map(fs -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fs.getId());
            map.put("flashPrice", fs.getFlashPrice());
            map.put("flashStock", fs.getFlashStock());
            map.put("soldCount", fs.getSoldCount());
            map.put("perLimit", fs.getPerLimit());
            map.put("startTime", fs.getStartTime());
            map.put("endTime", fs.getEndTime());
            map.put("status", fs.getStatus());
            Product product = productMapper.selectById(fs.getProductId());
            if (product != null) {
                map.put("productId", product.getId());
                map.put("productName", product.getName());
                map.put("originalPrice", product.getPrice());
                map.put("imageUrl", product.getImageUrl());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> participate(Long userId, Long flashSaleId, Integer quantity) {
        FlashSale fs = flashSaleMapper.selectById(flashSaleId);
        if (fs == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(fs.getStartTime())) {
            throw new BusinessException("秒杀尚未开始");
        }
        if (now.isAfter(fs.getEndTime())) {
            throw new BusinessException("秒杀已结束");
        }
        if (fs.getSoldCount() >= fs.getFlashStock()) {
            throw new BusinessException("秒杀商品已售罄");
        }

        // 检查限购
        Long boughtCount = flashSaleOrderMapper.selectCount(
                new LambdaQueryWrapper<FlashSaleOrder>()
                        .eq(FlashSaleOrder::getUserId, userId)
                        .eq(FlashSaleOrder::getFlashSaleId, flashSaleId)
                        .eq(FlashSaleOrder::getStatus, 1)
        );
        if (boughtCount + quantity > fs.getPerLimit()) {
            throw new BusinessException("超过限购数量");
        }

        if (fs.getSoldCount() + quantity > fs.getFlashStock()) {
            throw new BusinessException("库存不足");
        }

        FlashSaleOrder order = new FlashSaleOrder();
        order.setUserId(userId);
        order.setFlashSaleId(flashSaleId);
        order.setProductId(fs.getProductId());
        order.setQuantity(quantity);
        order.setTotalAmount(fs.getFlashPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0);
        flashSaleOrderMapper.insert(order);

        fs.setSoldCount(fs.getSoldCount() + quantity);
        flashSaleMapper.updateById(fs);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("totalAmount", order.getTotalAmount());
        return result;
    }

    public List<FlashSaleOrder> getUserFlashSaleOrders(Long userId) {
        return flashSaleOrderMapper.selectList(
                new LambdaQueryWrapper<FlashSaleOrder>()
                        .eq(FlashSaleOrder::getUserId, userId)
                        .orderByDesc(FlashSaleOrder::getCreatedAt)
        );
    }

    @Transactional
    public void payFlashSaleOrder(Long userId, Long orderId) {
        FlashSaleOrder order = flashSaleOrderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus(1);
        flashSaleOrderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() - order.getQuantity());
            productMapper.updateById(product);
            productService.evictProductCache(order.getProductId());
        }
    }

    @Transactional
    public void cancelFlashSaleOrder(Long userId, Long orderId) {
        FlashSaleOrder order = flashSaleOrderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付订单可以取消");
        }

        FlashSale fs = flashSaleMapper.selectById(order.getFlashSaleId());
        if (fs != null) {
            fs.setSoldCount(Math.max(0, fs.getSoldCount() - order.getQuantity()));
            flashSaleMapper.updateById(fs);
        }

        order.setStatus(2);
        flashSaleOrderMapper.updateById(order);
    }
}
