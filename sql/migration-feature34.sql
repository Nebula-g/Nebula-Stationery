-- ============================================
-- 星云商城 功能3+4 增量建表
-- 功能3：多规格商品（product_spec, product_sku）
-- 功能4：收货地址（user_address）、退款售后（refund）
-- ============================================

USE `nebula-store`;

-- -------------------------------------------
-- 18. 商品规格表（product_spec）
-- 存储商品的规格名称，如"颜色"、"尺寸"
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `product_spec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `spec_name` VARCHAR(50) NOT NULL COMMENT '规格名称（如：颜色、尺寸）',
  `spec_values` VARCHAR(500) NOT NULL COMMENT '规格值列表（JSON数组，如：["红色","蓝色","黑色"]）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格表';

-- -------------------------------------------
-- 19. 商品SKU表（product_sku）
-- 存储具体的SKU组合，如"红色-XL"
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `sku_code` VARCHAR(100) DEFAULT NULL COMMENT 'SKU编码',
  `spec_values` VARCHAR(500) NOT NULL COMMENT '规格值组合（JSON，如：{"颜色":"红色","尺寸":"XL"}）',
  `spec_text` VARCHAR(200) NOT NULL COMMENT '规格文字描述（如：红色-XL）',
  `price` DECIMAL(10,2) NOT NULL COMMENT 'SKU价格',
  `stock` INT NOT NULL DEFAULT 0 COMMENT 'SKU库存',
  `image_url` VARCHAR(200) DEFAULT NULL COMMENT 'SKU图片（规格图）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- -------------------------------------------
-- 20. 用户收货地址表（user_address）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `user_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) NOT NULL COMMENT '省',
  `city` VARCHAR(50) NOT NULL COMMENT '市',
  `district` VARCHAR(50) NOT NULL COMMENT '区/县',
  `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

-- -------------------------------------------
-- 21. 退款售后表（refund）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refund_no` VARCHAR(50) NOT NULL COMMENT '退款单号',
  `order_id` BIGINT NOT NULL COMMENT '订单 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `type` TINYINT NOT NULL COMMENT '类型：1-仅退款，2-退货退款',
  `reason` VARCHAR(200) NOT NULL COMMENT '退款原因',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已同意，2-已拒绝，3-退款中，4-已完成',
  `admin_remark` VARCHAR(200) DEFAULT NULL COMMENT '管理员备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款售后表';
