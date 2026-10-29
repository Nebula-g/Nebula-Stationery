package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.entity.Logistics;
import com.nebula.stationery.entity.LogisticsTrack;
import com.nebula.stationery.logistics.Kuaidi100Provider;
import com.nebula.stationery.logistics.LogisticsProvider;
import com.nebula.stationery.logistics.MockLogisticsProvider;
import com.nebula.stationery.mapper.LogisticsMapper;
import com.nebula.stationery.mapper.LogisticsTrackMapper;
import com.nebula.stationery.vo.LogisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private static final Map<String, String> COMPANIES = new LinkedHashMap<>();

    static {
        COMPANIES.put("SF", "顺丰速运");
        COMPANIES.put("YTO", "圆通速递");
        COMPANIES.put("ZTO", "中通快递");
        COMPANIES.put("YUNDA", "韵达快递");
        COMPANIES.put("STO", "申通快递");
        COMPANIES.put("JD", "京东物流");
    }

    private final LogisticsMapper logisticsMapper;
    private final LogisticsTrackMapper trackMapper;
    private final MockLogisticsProvider mockProvider;
    private final Kuaidi100Provider kuaidi100Provider;

    @Value("${stationery.logistics.provider:mock}")
    private String providerName;

    /** 后台发货时调用，已有物流单则直接复用 */
    public Logistics ship(Long orderId, String companyCode, String trackingNo, String receiverPhone) {
        Logistics existing = findByOrder(orderId);
        if (existing != null) {
            return existing;
        }

        String code = companyCode == null || companyCode.isBlank() ? "SF" : companyCode.trim().toUpperCase();
        Logistics logistics = new Logistics();
        logistics.setOrderId(orderId);
        logistics.setCompanyCode(code);
        logistics.setCompanyName(COMPANIES.getOrDefault(code, "星云专线"));
        logistics.setTrackingNo(trackingNo == null || trackingNo.isBlank() ? generateTrackingNo(code) : trackingNo.trim());
        logistics.setReceiverPhone(receiverPhone);
        logistics.setStatus(0);
        logistics.setShippedAt(LocalDateTime.now());
        logisticsMapper.insert(logistics);
        return logistics;
    }

    public Logistics findByOrder(Long orderId) {
        return logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId, orderId).last("LIMIT 1")
        );
    }

    /** 订单详情用的物流视图：优先查真实接口，失败时用最近一次落库的轨迹兜底 */
    public LogisticsVO getView(Long orderId) {
        Logistics logistics = findByOrder(orderId);
        if (logistics == null) {
            return null;
        }

        List<LogisticsProvider.TrackPoint> points = new ArrayList<>();
        if (!"mock".equalsIgnoreCase(providerName)) {
            points = kuaidi100Provider.query(
                    logistics.getCompanyCode(), logistics.getTrackingNo(),
                    logistics.getReceiverPhone(), logistics.getShippedAt());
        }
        if (points.isEmpty()) {
            points = mockProvider.query(
                    logistics.getCompanyCode(), logistics.getTrackingNo(),
                    logistics.getReceiverPhone(), logistics.getShippedAt());
        }

        saveTracks(logistics, points);

        LogisticsVO vo = new LogisticsVO();
        vo.setCompanyCode(logistics.getCompanyCode());
        vo.setCompanyName(logistics.getCompanyName());
        vo.setTrackingNo(logistics.getTrackingNo());
        vo.setStatus(logistics.getStatus());
        vo.setStatusText(logistics.getStatus() != null && logistics.getStatus() == 1 ? "已签收" : "运输中");
        vo.setShippedAt(logistics.getShippedAt());
        vo.setProvider("mock".equalsIgnoreCase(providerName) ? "模拟物流" : "快递100");

        List<LogisticsVO.TrackVO> tracks = new ArrayList<>();
        points.stream()
                .sorted(Comparator.comparing(LogisticsProvider.TrackPoint::time).reversed())
                .forEach(p -> {
                    LogisticsVO.TrackVO track = new LogisticsVO.TrackVO();
                    track.setTime(p.time());
                    track.setLocation(p.location());
                    track.setDescription(p.description());
                    tracks.add(track);
                });
        vo.setTracks(tracks);
        return vo;
    }

    private void saveTracks(Logistics logistics, List<LogisticsProvider.TrackPoint> points) {
        long stored = trackMapper.selectCount(
                new LambdaQueryWrapper<LogisticsTrack>().eq(LogisticsTrack::getLogisticsId, logistics.getId())
        );
        if (stored == points.size() && stored > 0) {
            return;
        }

        trackMapper.delete(new LambdaQueryWrapper<LogisticsTrack>()
                .eq(LogisticsTrack::getLogisticsId, logistics.getId()));
        for (LogisticsProvider.TrackPoint point : points) {
            LogisticsTrack track = new LogisticsTrack();
            track.setLogisticsId(logistics.getId());
            track.setTrackTime(point.time());
            track.setDescription(point.description());
            track.setLocation(point.location());
            trackMapper.insert(track);
        }

        boolean delivered = !points.isEmpty()
                && points.get(points.size() - 1).description().contains("签收");
        int status = delivered ? 1 : 0;
        if (logistics.getStatus() == null || logistics.getStatus() != status) {
            logistics.setStatus(status);
            logisticsMapper.updateById(logistics);
        }
    }

    public List<String> companies() {
        return COMPANIES.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .toList();
    }

    private String generateTrackingNo(String companyCode) {
        String digits = String.valueOf(System.currentTimeMillis()).substring(3)
                + String.format("%03d", new SecureRandom().nextInt(1000));
        return companyCode + digits;
    }

    public String companyName(String code) {
        if (code == null) {
            throw new BusinessException("请选择快递公司");
        }
        String name = COMPANIES.get(code.toUpperCase());
        if (name == null) {
            throw new BusinessException("不支持的快递公司：" + code);
        }
        return name;
    }
}
