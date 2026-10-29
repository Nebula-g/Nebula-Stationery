package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.CartRequest;
import com.nebula.stationery.entity.CartItem;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.entity.ProductSku;
import com.nebula.stationery.mapper.CartItemMapper;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.ProductSkuMapper;
import com.nebula.stationery.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Transactional
    public void addToCart(Long userId, CartRequest request) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }

        BigDecimal price = product.getPrice();
        int stock = product.getStock();
        String specText = null;

        if (request.getSkuId() != null) {
            ProductSku sku = productSkuMapper.selectById(request.getSkuId());
            if (sku == null || sku.getStatus() == 0 || !sku.getProductId().equals(request.getProductId())) {
                throw new BusinessException("SKU不存在或已禁用");
            }
            price = sku.getPrice();
            stock = sku.getStock();
            specText = sku.getSpecText();
        }

        if (stock < request.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, request.getProductId());
        if (request.getSkuId() != null) {
            wrapper.eq(CartItem::getSkuId, request.getSkuId());
        } else {
            wrapper.isNull(CartItem::getSkuId);
        }
        CartItem existingItem = cartItemMapper.selectOne(wrapper);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (stock < newQuantity) {
                throw new BusinessException("库存不足");
            }
            existingItem.setQuantity(newQuantity);
            cartItemMapper.updateById(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setSkuId(request.getSkuId());
            cartItem.setQuantity(request.getQuantity());
            cartItemMapper.insert(cartItem);
        }
    }

    public List<CartItemVO> getCartList(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
        );

        return cartItems.stream().map(item -> {
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setSkuId(item.getSkuId());
            vo.setQuantity(item.getQuantity());

            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setImageUrl(product.getImageUrl());
                vo.setStatus(product.getStatus());

                if (item.getSkuId() != null) {
                    ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                    if (sku != null) {
                        vo.setPrice(sku.getPrice());
                        vo.setSpecText(sku.getSpecText());
                        vo.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                        if (sku.getImageUrl() != null && !sku.getImageUrl().isEmpty()) {
                            vo.setImageUrl(sku.getImageUrl());
                        }
                    }
                } else {
                    vo.setPrice(product.getPrice());
                    vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateCartItem(Long userId, Long itemId, Integer quantity) {
        CartItem cartItem = cartItemMapper.selectById(itemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }

        int stock;
        if (cartItem.getSkuId() != null) {
            ProductSku sku = productSkuMapper.selectById(cartItem.getSkuId());
            stock = sku != null ? sku.getStock() : 0;
        } else {
            Product product = productMapper.selectById(cartItem.getProductId());
            stock = product != null ? product.getStock() : 0;
        }

        if (stock < quantity) {
            throw new BusinessException("库存不足");
        }

        cartItem.setQuantity(quantity);
        cartItemMapper.updateById(cartItem);
    }

    @Transactional
    public void deleteCartItem(Long userId, Long itemId) {
        CartItem cartItem = cartItemMapper.selectById(itemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        cartItemMapper.deleteById(itemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
        );
    }
}
