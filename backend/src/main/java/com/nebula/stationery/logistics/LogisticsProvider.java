package com.nebula.stationery.logistics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流查询抽象：默认模拟通道（按发货时间推移生成轨迹），
 * 配置 stationery.logistics.provider=kuaidi100 且填好 key/customer 后走快递100实时查询。
 */
public interface LogisticsProvider {

    String name();

    boolean available();

    List<TrackPoint> query(String companyCode, String trackingNo, String receiverPhone, LocalDateTime shippedAt);

    record TrackPoint(LocalDateTime time, String location, String description) {
    }
}
