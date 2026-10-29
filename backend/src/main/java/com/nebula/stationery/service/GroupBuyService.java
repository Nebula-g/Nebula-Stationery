package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.entity.GroupBuy;
import com.nebula.stationery.entity.GroupParticipant;
import com.nebula.stationery.entity.GroupSession;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.mapper.GroupBuyMapper;
import com.nebula.stationery.mapper.GroupParticipantMapper;
import com.nebula.stationery.mapper.GroupSessionMapper;
import com.nebula.stationery.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyMapper groupBuyMapper;
    private final GroupSessionMapper groupSessionMapper;
    private final GroupParticipantMapper groupParticipantMapper;
    private final ProductMapper productMapper;

    public List<Map<String, Object>> listGroupBuys() {
        List<GroupBuy> buys = groupBuyMapper.selectList(null);
        return buys.stream().map(gb -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", gb.getId());
            map.put("groupPrice", gb.getGroupPrice());
            map.put("minGroupSize", gb.getMinGroupSize());
            map.put("maxGroupSize", gb.getMaxGroupSize());
            Product product = productMapper.selectById(gb.getProductId());
            if (product != null) {
                map.put("productId", product.getId());
                map.put("productName", product.getName());
                map.put("originalPrice", product.getPrice());
                map.put("imageUrl", product.getImageUrl());
            }
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> listSessions(Long groupBuyId) {
        List<GroupSession> sessions = groupSessionMapper.selectList(
                new LambdaQueryWrapper<GroupSession>()
                        .eq(GroupSession::getGroupBuyId, groupBuyId)
                        .eq(GroupSession::getStatus, 0)
                        .ge(GroupSession::getExpireTime, LocalDateTime.now())
        );
        return sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("currentSize", s.getCurrentSize());
            map.put("minSize", s.getMinSize());
            map.put("status", s.getStatus());
            map.put("expireTime", s.getExpireTime());
            map.put("createdAt", s.getCreatedAt());
            List<GroupParticipant> participants = groupParticipantMapper.selectList(
                    new LambdaQueryWrapper<GroupParticipant>()
                            .eq(GroupParticipant::getGroupSessionId, s.getId())
            );
            map.put("participants", participants.stream().map(p -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("userId", p.getUserId());
                pm.put("isLeader", p.getIsLeader());
                pm.put("joinedAt", p.getJoinedAt());
                return pm;
            }).collect(Collectors.toList()));
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createSession(Long userId, Long groupBuyId) {
        GroupBuy gb = groupBuyMapper.selectById(groupBuyId);
        if (gb == null) {
            throw new BusinessException("拼团商品不存在");
        }

        GroupSession session = new GroupSession();
        session.setGroupBuyId(groupBuyId);
        session.setProductId(gb.getProductId());
        session.setLeaderId(userId);
        session.setCurrentSize(1);
        session.setMinSize(gb.getMinGroupSize());
        session.setStatus(0);
        session.setExpireTime(LocalDateTime.now().plusHours(24));
        groupSessionMapper.insert(session);

        GroupParticipant participant = new GroupParticipant();
        participant.setGroupSessionId(session.getId());
        participant.setUserId(userId);
        participant.setIsLeader(1);
        groupParticipantMapper.insert(participant);

        gb.setTotalGroups(gb.getTotalGroups() + 1);
        groupBuyMapper.updateById(gb);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("expireTime", session.getExpireTime());
        return result;
    }

    @Transactional
    public Map<String, Object> joinSession(Long userId, Long sessionId) {
        GroupSession session = groupSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("拼团会话不存在");
        }
        if (session.getStatus() != 0) {
            throw new BusinessException("拼团已结束");
        }
        if (LocalDateTime.now().isAfter(session.getExpireTime())) {
            session.setStatus(2);
            groupSessionMapper.updateById(session);
            throw new BusinessException("拼团已过期");
        }
        if (session.getCurrentSize() >= session.getMinSize()) {
            throw new BusinessException("拼团人数已满");
        }

        // 检查是否已参团
        Long existing = groupParticipantMapper.selectCount(
                new LambdaQueryWrapper<GroupParticipant>()
                        .eq(GroupParticipant::getGroupSessionId, sessionId)
                        .eq(GroupParticipant::getUserId, userId)
        );
        if (existing > 0) {
            throw new BusinessException("您已参与此拼团");
        }

        GroupParticipant participant = new GroupParticipant();
        participant.setGroupSessionId(sessionId);
        participant.setUserId(userId);
        participant.setIsLeader(0);
        groupParticipantMapper.insert(participant);

        session.setCurrentSize(session.getCurrentSize() + 1);
        if (session.getCurrentSize() >= session.getMinSize()) {
            session.setStatus(1);
        }
        groupSessionMapper.updateById(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("currentSize", session.getCurrentSize());
        result.put("status", session.getStatus());
        return result;
    }

    public Map<String, Object> getSessionDetail(Long sessionId) {
        GroupSession session = groupSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("拼团会话不存在");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("currentSize", session.getCurrentSize());
        map.put("minSize", session.getMinSize());
        map.put("status", session.getStatus());
        map.put("expireTime", session.getExpireTime());
        map.put("leaderId", session.getLeaderId());

        GroupBuy gb = groupBuyMapper.selectById(session.getGroupBuyId());
        Product product = productMapper.selectById(session.getProductId());
        if (gb != null && product != null) {
            map.put("productName", product.getName());
            map.put("groupPrice", gb.getGroupPrice());
            map.put("imageUrl", product.getImageUrl());
        }

        List<GroupParticipant> participants = groupParticipantMapper.selectList(
                new LambdaQueryWrapper<GroupParticipant>()
                        .eq(GroupParticipant::getGroupSessionId, sessionId)
        );
        map.put("participants", participants.size());
        return map;
    }
}
