package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.service.ExcelExportService;
import com.nebula.stationery.service.StatsService;
import com.nebula.stationery.vo.CategorySalesVO;
import com.nebula.stationery.vo.DailySalesVO;
import com.nebula.stationery.vo.LowStockVO;
import com.nebula.stationery.vo.ProductSalesVO;
import com.nebula.stationery.vo.StatOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;
    private final ExcelExportService excelExportService;

    @GetMapping("/stats/overview")
    public Result<StatOverviewVO> overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/stats/sales-trend")
    public Result<List<DailySalesVO>> salesTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(statsService.salesTrend(days));
    }

    @GetMapping("/stats/top-products")
    public Result<List<ProductSalesVO>> topProducts(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(statsService.topProducts(limit));
    }

    @GetMapping("/stats/category-sales")
    public Result<List<CategorySalesVO>> categorySales() {
        return Result.success(statsService.categorySales());
    }

    @GetMapping("/stats/low-stock")
    public Result<List<LowStockVO>> lowStock() {
        return Result.success(statsService.lowStockProducts());
    }

    @GetMapping("/export/orders")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
            throws IOException {
        byte[] body = excelExportService.exportOrders(status, startDate, endDate);
        return excelFile("订单明细.xlsx", body);
    }

    @GetMapping("/export/products")
    public ResponseEntity<byte[]> exportProducts() throws IOException {
        return excelFile("商品销量.xlsx", excelExportService.exportProducts());
    }

    @GetMapping("/export/sales-report")
    public ResponseEntity<byte[]> exportSalesReport(@RequestParam(defaultValue = "30") Integer days)
            throws IOException {
        return excelFile("销售报表.xlsx", excelExportService.exportSalesReport(days));
    }

    private ResponseEntity<byte[]> excelFile(String fileName, byte[] body) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
