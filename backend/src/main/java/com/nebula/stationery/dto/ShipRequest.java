package com.nebula.stationery.dto;

import lombok.Data;

@Data
public class ShipRequest {

    /** 快递公司编码：SF/YTO/ZTO/YUNDA/STO/JD，留空默认顺丰 */
    private String companyCode;

    /** 运单号，留空自动生成 */
    private String trackingNo;
}
