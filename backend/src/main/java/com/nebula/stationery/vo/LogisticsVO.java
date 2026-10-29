package com.nebula.stationery.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogisticsVO {

    private String companyCode;
    private String companyName;
    private String trackingNo;
    private Integer status;
    private String statusText;
    private LocalDateTime shippedAt;
    private String provider;
    private List<TrackVO> tracks;

    @Data
    public static class TrackVO {
        private LocalDateTime time;
        private String location;
        private String description;
    }
}
