package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nebula.stationery.entity.Category;
import com.nebula.stationery.entity.Order;
import com.nebula.stationery.entity.OrderItem;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.mapper.CategoryMapper;
import com.nebula.stationery.mapper.OrderItemMapper;
import com.nebula.stationery.mapper.OrderMapper;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.UserMapper;
import com.nebula.stationery.vo.CategorySalesVO;
import com.nebula.stationery.vo.DailySalesVO;
import com.nebula.stationery.vo.LowStockVO;
import com.nebula.stationery.vo.ProductSalesVO;
import com.nebula.stationery.vo.StatOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    /** 1=已支付 2=已发货 3=已完成，只有这三类订单计入成交额 */
    private static final String PAID_ORDER_SUBQUERY = "SELECT id FROM `order` WHERE status IN (1,2,3)";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public StatOverviewVO overview() {
        StatOverviewVO vo = new StatOverviewVO();

        vo.setTotalOrderCount(orderMapper.selectCount(null).intValue());
        vo.setTotalRevenue(sumOrderAmount(paidOrders(null)));

        vo.setTodayOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, LocalDate.now().atStartOfDay())
        ).intValue());
        vo.setTodayRevenue(sumOrderAmount(paidOrders(LocalDate.now().atStartOfDay())));

        vo.setTotalUserCount(userMapper.selectCount(null).intValue());
        vo.setOnSaleProductCount(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        ).intValue());
        vo.setLowStockCount(productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .le(Product::getStock, LOW_STOCK_THRESHOLD)
        ).intValue());
        vo.setPendingShipCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, 1)
        ).intValue());

        return vo;
    }

    /** 近 N 天每日成交额与订单数，缺失的日期补 0，保证折线图 x 轴连续 */
    public List<DailySalesVO> salesTrend(int days) {
        int span = Math.max(1, Math.min(days, 90));
        LocalDate startDate = LocalDate.now().minusDays(span - 1L);

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(created_at) AS stat_date", "COUNT(*) AS order_count", "SUM(total_amount) AS revenue")
                .in("status", 1, 2, 3)
                .ge("created_at", startDate.atStartOfDay())
                .groupBy("DATE(created_at)");

        Map<String, Map<String, Object>> rowsByKey = new HashMap<>();
        for (Map<String, Object> row : orderMapper.selectMaps(wrapper)) {
            rowsByKey.put(String.valueOf(row.get("stat_date")), row);
        }

        List<DailySalesVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate day = startDate.plusDays(i);
            String key = day.format(DATE_FORMAT);
            Map<String, Object> row = rowsByKey.get(key);

            DailySalesVO vo = new DailySalesVO();
            vo.setDate(key);
            vo.setOrderCount(row == null ? 0 : toInt(row.get("order_count")));
            vo.setRevenue(row == null ? BigDecimal.ZERO : toDecimal(row.get("revenue")));
            result.add(vo);
        }
        return result;
    }

    /** 销量排行榜：按已支付订单的明细聚合 */
    public List<ProductSalesVO> topProducts(int limit) {
        int n = Math.max(1, Math.min(limit, 50));

        List<Map<String, Object>> rows = aggregatedItems(new QueryWrapper<OrderItem>()
                .select("product_id AS product_id", "SUM(quantity) AS sold_count", "SUM(subtotal) AS amount")
                .groupBy("product_id")
                .orderByDesc("sold_count")
                .last("LIMIT " + n));

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = rows.stream()
                .map(row -> toLong(row.get("product_id")))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Product> productMap = batchProducts(productIds);

        List<ProductSalesVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long productId = toLong(row.get("product_id"));
            Product product = productMap.get(productId);
            if (product == null) {
                continue;
            }
            ProductSalesVO vo = new ProductSalesVO();
            vo.setProductId(productId);
            vo.setName(product.getName());
            vo.setSoldCount(toInt(row.get("sold_count")));
            vo.setAmount(toDecimal(row.get("amount")));
            result.add(vo);
        }
        return result;
    }

    /** 各一级分类的销量占比，用于饼图 */
    public List<CategorySalesVO> categorySales() {
        List<Map<String, Object>> rows = aggregatedItems(new QueryWrapper<OrderItem>()
                .select("product_id AS product_id", "SUM(quantity) AS sold_count", "SUM(subtotal) AS amount")
                .groupBy("product_id"));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = rows.stream()
                .map(row -> toLong(row.get("product_id")))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Product> productMap = batchProducts(productIds);
        Map<Long, String> rootNameMap = rootCategoryNames();

        Map<String, CategorySalesVO> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Product product = productMap.get(toLong(row.get("product_id")));
            if (product == null) {
                continue;
            }
            String categoryName = rootNameMap.getOrDefault(product.getCategoryId(), "未分类");
            CategorySalesVO vo = grouped.computeIfAbsent(categoryName, name -> {
                CategorySalesVO created = new CategorySalesVO();
                created.setName(name);
                created.setSoldCount(0);
                created.setAmount(BigDecimal.ZERO);
                return created;
            });
            vo.setSoldCount(vo.getSoldCount() + toInt(row.get("sold_count")));
            vo.setAmount(vo.getAmount().add(toDecimal(row.get("amount"))));
        }

        return grouped.values().stream()
                .sorted((a, b) -> Integer.compare(b.getSoldCount(), a.getSoldCount()))
                .collect(Collectors.toList());
    }

    /** 低库存预警清单，后台补货用 */
    public List<LowStockVO> lowStockProducts() {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .le(Product::getStock, LOW_STOCK_THRESHOLD)
                .orderByAsc(Product::getStock));
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        return products.stream().map(product -> {
            LowStockVO vo = new LowStockVO();
            vo.setId(product.getId());
            vo.setName(product.getName());
            vo.setCategoryName(categoryNames.getOrDefault(product.getCategoryId(), "未分类"));
            vo.setStock(product.getStock());
            vo.setSoldCount(product.getSoldCount());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> aggregatedItems(QueryWrapper<OrderItem> wrapper) {
        wrapper.inSql("order_id", PAID_ORDER_SUBQUERY);
        return orderItemMapper.selectMaps(wrapper);
    }

    private List<Order> paidOrders(LocalDateTime since) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2, 3);
        if (since != null) {
            wrapper.ge(Order::getCreatedAt, since);
        }
        return orderMapper.selectList(wrapper);
    }

    private BigDecimal sumOrderAmount(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, Product> batchProducts(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    /** 二级分类向上归并到一级分类名 */
    private Map<Long, String> rootCategoryNames() {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, Category> byId = all.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        Map<Long, String> result = new HashMap<>();
        for (Category category : all) {
            Category node = category;
            int guard = 0;
            while (node.getParentId() != null && byId.containsKey(node.getParentId()) && guard++ < 5) {
                node = byId.get(node.getParentId());
            }
            result.put(category.getId(), node.getName());
        }
        return result;
    }

    private Integer toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }
}
