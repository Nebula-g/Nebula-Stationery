package com.nebula.stationery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.ProductRequest;
import com.nebula.stationery.dto.SkuRequest;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.entity.ProductSku;
import com.nebula.stationery.entity.ProductSpec;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.ProductSkuMapper;
import com.nebula.stationery.mapper.ProductSpecMapper;
import com.nebula.stationery.service.ProductService;
import com.nebula.stationery.vo.ProductSkuVO;
import com.nebula.stationery.vo.ProductSpecVO;
import com.nebula.stationery.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Result<Page<ProductVO>> getProductList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProductVO> result = productService.getProductList(categoryId, null, page, size);
        return Result.success(result);
    }

    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductRequest request) {
        Long id = productService.createProduct(request);
        return Result.success("新增成功", id);
    }

    @PutMapping("/{id}")
    public Result<?> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
        return Result.success("编辑成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<?> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateProductStatus(id, status);
        return Result.success("操作成功", null);
    }

    @GetMapping("/{id}/specs")
    public Result<List<ProductSpecVO>> getSpecs(@PathVariable Long id) {
        List<ProductSpec> specs = productSpecMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>()
                        .eq(ProductSpec::getProductId, id)
                        .orderByAsc(ProductSpec::getSortOrder)
        );
        List<ProductSpecVO> voList = specs.stream().map(spec -> {
            ProductSpecVO vo = new ProductSpecVO();
            vo.setId(spec.getId());
            vo.setSpecName(spec.getSpecName());
            vo.setSortOrder(spec.getSortOrder());
            try {
                vo.setSpecValues(objectMapper.readValue(spec.getSpecValues(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                vo.setSpecValues(Collections.emptyList());
            }
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    @PostMapping("/{id}/specs")
    @Transactional
    public Result<Long> addSpec(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("商品不存在");

        ProductSpec spec = new ProductSpec();
        spec.setProductId(id);
        spec.setSpecName((String) body.get("specName"));
        try {
            spec.setSpecValues(objectMapper.writeValueAsString(body.get("specValues")));
        } catch (Exception e) {
            throw new BusinessException("规格值格式错误");
        }
        spec.setSortOrder(body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : 0);
        productSpecMapper.insert(spec);
        return Result.success("添加成功", spec.getId());
    }

    @PutMapping("/{productId}/specs/{specId}")
    @Transactional
    public Result<?> updateSpec(@PathVariable Long productId, @PathVariable Long specId, @RequestBody Map<String, Object> body) {
        ProductSpec spec = productSpecMapper.selectById(specId);
        if (spec == null || !spec.getProductId().equals(productId)) throw new BusinessException("规格不存在");

        spec.setSpecName((String) body.get("specName"));
        try {
            spec.setSpecValues(objectMapper.writeValueAsString(body.get("specValues")));
        } catch (Exception e) {
            throw new BusinessException("规格值格式错误");
        }
        if (body.get("sortOrder") != null) spec.setSortOrder((Integer) body.get("sortOrder"));
        productSpecMapper.updateById(spec);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{productId}/specs/{specId}")
    @Transactional
    public Result<?> deleteSpec(@PathVariable Long productId, @PathVariable Long specId) {
        ProductSpec spec = productSpecMapper.selectById(specId);
        if (spec == null || !spec.getProductId().equals(productId)) throw new BusinessException("规格不存在");
        productSpecMapper.deleteById(specId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/skus")
    public Result<List<ProductSkuVO>> getSkus(@PathVariable Long id) {
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id)
        );
        List<ProductSkuVO> voList = skus.stream().map(sku -> {
            ProductSkuVO vo = new ProductSkuVO();
            vo.setId(sku.getId());
            vo.setSkuCode(sku.getSkuCode());
            vo.setSpecValues(sku.getSpecValues());
            vo.setSpecText(sku.getSpecText());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setImageUrl(sku.getImageUrl());
            vo.setStatus(sku.getStatus());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    @PostMapping("/{id}/skus")
    @Transactional
    public Result<Long> addSku(@PathVariable Long id, @Valid @RequestBody SkuRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("商品不存在");

        ProductSku sku = new ProductSku();
        sku.setProductId(id);
        sku.setSkuCode(request.getSkuCode());
        sku.setSpecValues(request.getSpecValues());
        sku.setSpecText(request.getSpecText());
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setImageUrl(request.getImageUrl());
        sku.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        productSkuMapper.insert(sku);
        return Result.success("添加成功", sku.getId());
    }

    @PutMapping("/{productId}/skus/{skuId}")
    @Transactional
    public Result<?> updateSku(@PathVariable Long productId, @PathVariable Long skuId, @Valid @RequestBody SkuRequest request) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(productId)) throw new BusinessException("SKU不存在");

        sku.setSkuCode(request.getSkuCode());
        sku.setSpecValues(request.getSpecValues());
        sku.setSpecText(request.getSpecText());
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) sku.setStatus(request.getStatus());
        productSkuMapper.updateById(sku);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{productId}/skus/{skuId}")
    @Transactional
    public Result<?> deleteSku(@PathVariable Long productId, @PathVariable Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(productId)) throw new BusinessException("SKU不存在");
        productSkuMapper.deleteById(skuId);
        return Result.success("删除成功", null);
    }
}
