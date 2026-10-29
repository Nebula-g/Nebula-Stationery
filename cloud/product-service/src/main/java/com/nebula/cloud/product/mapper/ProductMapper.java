package com.nebula.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nebula.cloud.product.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    /** 带库存条件的扣减，返回 0 表示库存不足，靠数据库保证不超卖 */
    @Update("UPDATE product SET stock = stock - #{quantity}, sold_count = sold_count + #{quantity} "
            + "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock + #{quantity}, sold_count = GREATEST(sold_count - #{quantity}, 0) "
            + "WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
