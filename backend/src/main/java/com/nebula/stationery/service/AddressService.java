package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.AddressRequest;
import com.nebula.stationery.entity.UserAddress;
import com.nebula.stationery.mapper.UserAddressMapper;
import com.nebula.stationery.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper userAddressMapper;

    public List<AddressVO> getAddressList(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt)
        );
        return addresses.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public AddressVO getAddressDetail(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        return convertToVO(address);
    }

    @Transactional
    public Long createAddress(Long userId, AddressRequest request) {
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        userAddressMapper.insert(address);
        return address.getId();
    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }

        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefault(userId);
        }

        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        userAddressMapper.updateById(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        userAddressMapper.deleteById(addressId);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        clearDefault(userId);
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
    }

    private void clearDefault(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getIsDefault, 1)
        );
        for (UserAddress addr : addresses) {
            addr.setIsDefault(0);
            userAddressMapper.updateById(addr);
        }
    }

    private AddressVO convertToVO(UserAddress address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setIsDefault(address.getIsDefault());
        vo.setFullAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        return vo;
    }
}
