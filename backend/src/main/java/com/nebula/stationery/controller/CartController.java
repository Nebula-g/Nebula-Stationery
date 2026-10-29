package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.CartRequest;
import com.nebula.stationery.service.CartService;
import com.nebula.stationery.vo.CartItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public Result<?> addToCart(Authentication authentication, @Valid @RequestBody CartRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.addToCart(userId, request);
        return Result.success("添加成功", null);
    }

    @GetMapping
    public Result<List<CartItemVO>> getCartList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CartItemVO> result = cartService.getCartList(userId);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<?> updateCartItem(Authentication authentication,
                                     @PathVariable Long id,
                                     @RequestParam Integer quantity) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.updateCartItem(userId, id, quantity);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteCartItem(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.deleteCartItem(userId, id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    public Result<?> clearCart(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.clearCart(userId);
        return Result.success("清空成功", null);
    }
}
