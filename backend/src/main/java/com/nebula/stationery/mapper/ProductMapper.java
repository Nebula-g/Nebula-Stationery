package com.nebula.stationery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nebula.stationery.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
