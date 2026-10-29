package com.nebula.stationery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nebula.stationery.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
