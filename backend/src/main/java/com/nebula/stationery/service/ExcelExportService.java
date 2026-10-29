package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.entity.Order;
import com.nebula.stationery.entity.OrderItem;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.entity.User;
import com.nebula.stationery.mapper.OrderItemMapper;
import com.nebula.stationery.mapper.OrderMapper;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.UserMapper;
import com.nebula.stationery.vo.CategorySalesVO;
import com.nebula.stationery.vo.DailySalesVO;
import com.nebula.stationery.vo.ProductSalesVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 后台报表导出，使用 Apache POI 生成 .xlsx */
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 已支付/已发货/已完成才算有效成交额 */
    private static final List<Integer> PAID_STATUSES = List.of(1, 2, 3);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final StatsService statsService;

    public byte[] exportOrders(Integer status, LocalDate startDate, LocalDate endDate) throws IOException {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(Order::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(Order::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        List<Order> orders = orderMapper.selectList(wrapper);

        Map<Long, List<OrderItem>> itemsByOrder;
        Map<Long, String> userNames;
        if (orders.isEmpty()) {
            itemsByOrder = Collections.emptyMap();
            userNames = Collections.emptyMap();
        } else {
            List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
            itemsByOrder = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                    .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
            userNames = userMapper.selectBatchIds(
                            orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        }

        List<List<Object>> data = new ArrayList<>();
        for (Order order : orders) {
            String detail = itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()).stream()
                    .map(item -> item.getProductName()
                            + (item.getSpecText() == null || item.getSpecText().isBlank() ? "" : "（" + item.getSpecText() + "）")
                            + " x" + item.getQuantity())
                    .collect(Collectors.joining("；"));
            data.add(List.of(
                    order.getOrderNo(),
                    userNames.getOrDefault(order.getUserId(), "未知"),
                    detail,
                    order.getTotalAmount(),
                    statusText(order.getStatus()),
                    order.getCreatedAt() == null ? "" : order.getCreatedAt().format(DATE_TIME)
            ));
        }

        BigDecimal revenue = orders.stream()
                .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toBytes("订单明细",
                List.of("订单号", "下单用户", "商品明细", "订单金额", "状态", "下单时间"),
                data, new int[]{20, 14, 46, 12, 10, 20},
                "共 " + orders.size() + " 笔订单，有效成交额 " + revenue.toPlainString() + " 元");
    }

    public byte[] exportProducts() throws IOException {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getSoldCount));

        List<List<Object>> data = new ArrayList<>();
        for (Product product : products) {
            data.add(List.of(
                    String.valueOf(product.getId()),
                    product.getName(),
                    String.valueOf(product.getCategoryId()),
                    product.getPrice(),
                    product.getStock(),
                    product.getSoldCount(),
                    product.getStatus() != null && product.getStatus() == 1 ? "上架" : "下架",
                    product.getCreatedAt() == null ? "" : product.getCreatedAt().format(DATE_TIME)
            ));
        }

        return toBytes("商品销量",
                List.of("商品ID", "商品名称", "分类ID", "单价", "库存", "累计销量", "状态", "创建时间"),
                data, new int[]{10, 32, 10, 12, 10, 12, 10, 20},
                "共 " + products.size() + " 个商品");
    }

    public byte[] exportSalesReport(int days) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            List<List<Object>> trendData = new ArrayList<>();
            for (DailySalesVO row : statsService.salesTrend(days)) {
                trendData.add(List.of(row.getDate(), row.getOrderCount(), row.getRevenue()));
            }
            writeSheet(workbook, headerStyle, bodyStyle, "近" + days + "天趋势",
                    List.of("日期", "订单数", "成交额"), trendData, new int[]{16, 12, 14});

            List<List<Object>> rankData = new ArrayList<>();
            int rank = 1;
            for (ProductSalesVO item : statsService.topProducts(20)) {
                rankData.add(List.of(rank++, item.getName(), item.getSoldCount(), item.getAmount()));
            }
            writeSheet(workbook, headerStyle, bodyStyle, "销量排行TOP20",
                    List.of("排名", "商品名称", "销量", "销售额"), rankData, new int[]{8, 32, 12, 14});

            List<List<Object>> categoryData = new ArrayList<>();
            for (CategorySalesVO item : statsService.categorySales()) {
                categoryData.add(List.of(item.getName(), item.getSoldCount(), item.getAmount()));
            }
            writeSheet(workbook, headerStyle, bodyStyle, "分类占比",
                    List.of("分类", "销量", "销售额"), categoryData, new int[]{20, 12, 14});

            return toBytes(workbook);
        }
    }

    private byte[] toBytes(String sheetName, List<String> headers, List<List<Object>> data,
                           int[] widths, String summary) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);
            Sheet sheet = writeSheet(workbook, headerStyle, bodyStyle, sheetName, headers, data, widths);

            Cell note = sheet.createRow(data.size() + 2).createCell(0);
            note.setCellValue(summary);
            note.setCellStyle(bodyStyle);
            return toBytes(workbook);
        }
    }

    private Sheet writeSheet(Workbook workbook, CellStyle headerStyle, CellStyle bodyStyle, String name,
                             List<String> headers, List<List<Object>> data, int[] widths) {
        Sheet sheet = workbook.createSheet(name);
        sheet.createFreezePane(0, 1);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        int rowIndex = 1;
        for (List<Object> line : data) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < line.size(); i++) {
                Cell cell = row.createCell(i);
                Object value = line.get(i);
                if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else {
                    cell.setCellValue(value == null ? "" : value.toString());
                }
                cell.setCellStyle(bodyStyle);
            }
        }
        return sheet;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyBorders(style);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
