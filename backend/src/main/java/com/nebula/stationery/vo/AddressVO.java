package com.nebula.stationery.vo;

import lombok.Data;

@Data
public class AddressVO {

    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
    private String fullAddress;
}
