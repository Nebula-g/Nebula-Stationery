package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.entity.PointLog;
import com.nebula.stationery.entity.PointRule;
import com.nebula.stationery.entity.UserPoint;
import com.nebula.stationery.mapper.PointLogMapper;
import com.nebula.stationery.mapper.PointRuleMapper;
import com.nebula.stationery.mapper.UserPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointMapper userPointMapper;
    private final PointLogMapper pointLogMapper;
    private final PointRuleMapper pointRuleMapper;

    public Map<String, Object> getUserPoints(Long userId) {
        UserPoint up = userPointMapper.selectOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );
        Map<String, Object> result = new HashMap<>();
        if (up != null) {
            result.put("totalPoints", up.getTotalPoints());
            result.put("availablePoints", up.getAvailablePoints());
            result.put("usedPoints", up.getUsedPoints());
        } else {
            result.put("totalPoints", 0);
            result.put("availablePoints", 0);
            result.put("usedPoints", 0);
        }
        return result;
    }

    public List<PointLog> getPointLog(Long userId) {
        return pointLogMapper.selectList(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getCreatedAt)
        );
    }

    @Transactional
    public void earnPointsFromOrder(Long userId, BigDecimal orderAmount, Long orderId) {
        PointRule rule = pointRuleMapper.selectOne(
                new LambdaQueryWrapper<PointRule>().eq(PointRule::getRuleKey, "order_points_rate")
        );
        int rate = rule != null ? rule.getRuleValue() : 10;
        int points = orderAmount.multiply(BigDecimal.valueOf(rate)).intValue();
        if (points <= 0) return;

        addPoints(userId, points, 1, orderId, "订单获得积分，消费" + orderAmount + "元");
    }

    @Transactional
    public Map<String, Object> signIn(Long userId) {
        // 检查今天是否已签到
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        Long todaySignInCount = pointLogMapper.selectCount(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .eq(PointLog::getType, 2)
                        .ge(PointLog::getCreatedAt, todayStart)
                        .lt(PointLog::getCreatedAt, todayEnd)
        );
        if (todaySignInCount > 0) {
            throw new BusinessException("今日已签到");
        }

        PointRule rule = pointRuleMapper.selectOne(
                new LambdaQueryWrapper<PointRule>().eq(PointRule::getRuleKey, "sign_in_points")
        );
        int points = rule != null ? rule.getRuleValue() : 5;

        addPoints(userId, points, 2, null, "每日签到");

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        return result;
    }

    @Transactional
    public Map<String, Object> redeemPoints(Long userId, Integer points) {
        PointRule minRule = pointRuleMapper.selectOne(
                new LambdaQueryWrapper<PointRule>().eq(PointRule::getRuleKey, "min_redeem_points")
        );
        int minPoints = minRule != null ? minRule.getRuleValue() : 100;
        if (points < minPoints) {
            throw new BusinessException("最低兑换" + minPoints + "积分");
        }

        UserPoint up = userPointMapper.selectOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );
        if (up == null || up.getAvailablePoints() < points) {
            throw new BusinessException("积分不足");
        }

        PointRule rateRule = pointRuleMapper.selectOne(
                new LambdaQueryWrapper<PointRule>().eq(PointRule::getRuleKey, "points_to_money_rate")
        );
        int rate = rateRule != null ? rateRule.getRuleValue() : 100;
        BigDecimal money = BigDecimal.valueOf(points).divide(BigDecimal.valueOf(rate), 2, BigDecimal.ROUND_HALF_UP);

        up.setAvailablePoints(up.getAvailablePoints() - points);
        up.setUsedPoints(up.getUsedPoints() + points);
        userPointMapper.updateById(up);

        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setPoints(-points);
        log.setType(3);
        log.setDescription("积分兑换，抵扣" + money + "元");
        pointLogMapper.insert(log);

        Map<String, Object> result = new HashMap<>();
        result.put("money", money);
        result.put("remainingPoints", up.getAvailablePoints());
        return result;
    }

    public List<Map<String, Object>> getRules() {
        List<PointRule> rules = pointRuleMapper.selectList(null);
        return rules.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ruleKey", r.getRuleKey());
            map.put("ruleValue", r.getRuleValue());
            map.put("description", r.getDescription());
            return map;
        }).collect(Collectors.toList());
    }

    private void addPoints(Long userId, int points, int type, Long orderId, String description) {
        UserPoint up = userPointMapper.selectOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );
        if (up == null) {
            up = new UserPoint();
            up.setUserId(userId);
            up.setTotalPoints(0);
            up.setAvailablePoints(0);
            up.setUsedPoints(0);
            userPointMapper.insert(up);
        }
        up.setTotalPoints(up.getTotalPoints() + points);
        up.setAvailablePoints(up.getAvailablePoints() + points);
        userPointMapper.updateById(up);

        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setPoints(points);
        log.setType(type);
        log.setRelatedOrderId(orderId);
        log.setDescription(description);
        pointLogMapper.insert(log);
    }
}
