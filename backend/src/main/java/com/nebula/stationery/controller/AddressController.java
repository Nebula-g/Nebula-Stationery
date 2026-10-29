package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.AddressRequest;
import com.nebula.stationery.service.AddressService;
import com.nebula.stationery.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> getAddressList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<AddressVO> result = addressService.getAddressList(userId);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<AddressVO> getAddressDetail(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        AddressVO result = addressService.getAddressDetail(userId, id);
        return Result.success(result);
    }

    @PostMapping
    public Result<Long> createAddress(Authentication authentication, @Valid @RequestBody AddressRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Long id = addressService.createAddress(userId, request);
        return Result.success("添加成功", id);
    }

    @PutMapping("/{id}")
    public Result<?> updateAddress(Authentication authentication, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        addressService.updateAddress(userId, id, request);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteAddress(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        addressService.deleteAddress(userId, id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    public Result<?> setDefault(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        addressService.setDefault(userId, id);
        return Result.success("设置成功", null);
    }
}
