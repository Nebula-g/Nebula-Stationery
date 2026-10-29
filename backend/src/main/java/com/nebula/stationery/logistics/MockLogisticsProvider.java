package com.nebula.stationery.logistics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟物流：按发货时间每「一站」推进一分钟，六站后自动签收。
 * 每次查询都重新计算，所以页面刷新就能看到轨迹往前走，方便课堂演示。
 */
@Slf4j
@Component
public class MockLogisticsProvider implements LogisticsProvider {

    private static final String[][] STAGES = {
            {"杭州市", "快件已揽收，杭州西湖区文三路营业点"},
            {"杭州市", "快件已发出，离开杭州转运中心"},
            {"上海市", "快件到达上海转运中心，正在分拣"},
            {"南京市", "快件已到达南京转运中心"},
            {"南京市", "快递员正在派送，请保持电话畅通"},
            {"南京市", "已签收，感谢使用星云商城，期待再次为您服务"}
    };

    @Value("${stationery.logistics.mock.stage-minutes:1}")
    private long stageMinutes;

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<TrackPoint> query(String companyCode, String trackingNo, String receiverPhone, LocalDateTime shippedAt) {
        List<TrackPoint> points = new ArrayList<>();
        LocalDateTime base = shippedAt == null ? LocalDateTime.now() : shippedAt;
        LocalDateTime now = LocalDateTime.now();
        long minutes = Math.max(1, stageMinutes);

        for (int i = 0; i < STAGES.length; i++) {
            LocalDateTime time = base.plusMinutes(minutes * i);
            if (time.isAfter(now)) {
                break;
            }
            points.add(new TrackPoint(time, STAGES[i][0], STAGES[i][1]));
        }
        log.debug("模拟物流 {} 返回 {} 条轨迹", trackingNo, points.size());
        return points;
    }
}
