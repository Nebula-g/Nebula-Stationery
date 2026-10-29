package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.entity.Coupon;
import com.nebula.stationery.entity.UserCoupon;
import com.nebula.stationery.mapper.CouponMapper;
import com.nebula.stationery.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public List<Coupon> listAvailableCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, 1)
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now)
        );
    }

    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == 0) {
            throw new BusinessException("优惠券不存在或已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("优惠券不在有效期内");
        }
        if (coupon.getUsedCount() >= coupon.getTotalCount()) {
            throw new BusinessException("优惠券已领完");
        }

        // 检查每人限领
        Long claimedCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (claimedCount >= coupon.getPerLimit()) {
            throw new BusinessException("已达到领取上限");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponMapper.updateById(coupon);
    }

    public List<Map<String, Object>> getUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);

        return userCoupons.stream().map(uc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", uc.getId());
            map.put("status", uc.getStatus());
            map.put("createdAt", uc.getCreatedAt());
            map.put("usedAt", uc.getUsedAt());
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon != null) {
                map.put("couponName", coupon.getName());
                map.put("type", coupon.getType());
                map.put("discountValue", coupon.getDiscountValue());
                map.put("minAmount", coupon.getMinAmount());
                map.put("endTime", coupon.getEndTime());
            }
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Map<String, Object> applyCoupon(Long userId, Long userCouponId, java.math.BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (userCoupon.getStatus() != 0) {
            throw new BusinessException("优惠券已使用或已过期");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException("优惠券模板不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(coupon.getEndTime())) {
            userCoupon.setStatus(2);
            userCouponMapper.updateById(userCoupon);
            throw new BusinessException("优惠券已过期");
        }
        if (orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException("订单金额未满足使用门槛");
        }

        java.math.BigDecimal discount;
        if (coupon.getType() == 1) {
            discount = coupon.getDiscountValue();
        } else {
            discount = orderAmount.multiply(java.math.BigDecimal.ONE.subtract(coupon.getDiscountValue()));
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("discount", discount);
        result.put("finalAmount", orderAmount.subtract(discount));
        return result;
    }
}
