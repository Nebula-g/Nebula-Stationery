package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.RefundRequest;
import com.nebula.stationery.entity.Order;
import com.nebula.stationery.entity.Refund;
import com.nebula.stationery.mapper.OrderMapper;
import com.nebula.stationery.mapper.RefundMapper;
import com.nebula.stationery.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public Long applyRefund(Long userId, RefundRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1 && order.getStatus() != 3) {
            throw new BusinessException("只有已支付或已完成的订单可以申请退款");
        }

        List<Refund> existing = refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderId, request.getOrderId())
                        .in(Refund::getStatus, 0, 1, 3)
        );
        if (!existing.isEmpty()) {
            throw new BusinessException("该订单已有进行中的退款申请");
        }

        if (request.getRefundAmount().compareTo(order.getTotalAmount()) > 0) {
            throw new BusinessException("退款金额不能超过订单总额");
        }

        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(request.getOrderId());
        refund.setUserId(userId);
        refund.setType(request.getType());
        refund.setReason(request.getReason());
        refund.setDescription(request.getDescription());
        refund.setRefundAmount(request.getRefundAmount());
        refund.setStatus(0);
        refundMapper.insert(refund);
        return refund.getId();
    }

    public List<RefundVO> getUserRefunds(Long userId) {
        List<Refund> refunds = refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getUserId, userId)
                        .orderByDesc(Refund::getCreatedAt)
        );
        return refunds.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public RefundVO getRefundDetail(Long userId, Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BusinessException("退款单不存在");
        }
        return convertToVO(refund);
    }

    @Transactional
    public void cancelRefund(Long userId, Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BusinessException("退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException("只有待审核的退款可以取消");
        }
        refund.setStatus(2);
        refund.setAdminRemark("用户主动取消");
        refundMapper.updateById(refund);
    }

    // 管理员方法
    public Page<RefundVO> getAllRefunds(Integer status, Integer page, Integer size) {
        Page<Refund> refundPage = new Page<>(page, size);
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Refund::getStatus, status);
        }
        wrapper.orderByDesc(Refund::getCreatedAt);
        refundMapper.selectPage(refundPage, wrapper);

        Page<RefundVO> voPage = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        List<RefundVO> voList = refundPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Transactional
    public void approveRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException("只有待审核的退款可以审批");
        }
        refund.setStatus(3);
        refundMapper.updateById(refund);

        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null && order.getStatus() == 1) {
            order.setStatus(4);
            orderMapper.updateById(order);
        }

        refund.setStatus(4);
        refundMapper.updateById(refund);
    }

    @Transactional
    public void rejectRefund(Long refundId, String remark) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException("只有待审核的退款可以拒绝");
        }
        refund.setStatus(2);
        refund.setAdminRemark(remark);
        refundMapper.updateById(refund);
    }

    private RefundVO convertToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setType(refund.getType());
        vo.setTypeText(refund.getType() == 1 ? "仅退款" : "退货退款");
        vo.setReason(refund.getReason());
        vo.setDescription(refund.getDescription());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setStatus(refund.getStatus());
        vo.setStatusText(getStatusText(refund.getStatus()));
        vo.setAdminRemark(refund.getAdminRemark());
        vo.setCreatedAt(refund.getCreatedAt());
        vo.setUpdatedAt(refund.getUpdatedAt());

        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }
        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已同意";
            case 2 -> "已拒绝";
            case 3 -> "退款中";
            case 4 -> "已完成";
            default -> "未知";
        };
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
    }
}
