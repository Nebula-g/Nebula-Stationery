USE `nebula-store`;

-- 8. 优惠券模板表
CREATE TABLE IF NOT EXISTS `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `type` TINYINT NOT NULL COMMENT '类型：1-满减，2-折扣',
  `discount_value` DECIMAL(10,2) NOT NULL COMMENT '优惠值',
  `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '发放总量',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  `start_time` DATETIME NOT NULL COMMENT '有效期开始',
  `end_time` DATETIME NOT NULL COMMENT '有效期结束',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 9. 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券模板 ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期',
  `used_order_id` BIGINT DEFAULT NULL COMMENT '使用的订单 ID',
  `used_at` DATETIME DEFAULT NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 10. 秒杀商品表
CREATE TABLE IF NOT EXISTS `flash_sale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `flash_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `flash_stock` INT NOT NULL COMMENT '秒杀库存',
  `sold_count` INT NOT NULL DEFAULT 0 COMMENT '已售数量',
  `per_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-未开始，1-进行中，2-已结束',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品表';

-- 11. 秒杀订单表
CREATE TABLE IF NOT EXISTS `flash_sale_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `flash_sale_id` BIGINT NOT NULL COMMENT '秒杀活动 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_flash_sale_id` (`flash_sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';

-- 12. 拼团商品表
CREATE TABLE IF NOT EXISTS `group_buy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `group_price` DECIMAL(10,2) NOT NULL COMMENT '拼团价',
  `min_group_size` INT NOT NULL DEFAULT 2 COMMENT '成团最少人数',
  `max_group_size` INT NOT NULL DEFAULT 10 COMMENT '成团最多人数',
  `total_groups` INT NOT NULL DEFAULT 0 COMMENT '总开团数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼团商品表';

-- 13. 拼团会话表
CREATE TABLE IF NOT EXISTS `group_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_buy_id` BIGINT NOT NULL COMMENT '拼团商品 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `leader_id` BIGINT NOT NULL COMMENT '团长用户 ID',
  `current_size` INT NOT NULL DEFAULT 1 COMMENT '当前参团人数',
  `min_size` INT NOT NULL COMMENT '成团最少人数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-拼团中，1-已成团，2-已失败',
  `expire_time` DATETIME NOT NULL COMMENT '拼团截止时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_buy_id` (`group_buy_id`),
  KEY `idx_leader_id` (`leader_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼团会话表';

-- 14. 拼团参与表
CREATE TABLE IF NOT EXISTS `group_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_session_id` BIGINT NOT NULL COMMENT '拼团会话 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `is_leader` TINYINT NOT NULL DEFAULT 0 COMMENT '是否团长',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单 ID',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_session_id` (`group_session_id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_session_user` (`group_session_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼团参与表';

-- 15. 积分流水表
CREATE TABLE IF NOT EXISTS `point_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `points` INT NOT NULL COMMENT '积分变化量',
  `type` TINYINT NOT NULL COMMENT '类型：1-下单获得，2-签到获得，3-兑换消耗，4-退款退回',
  `related_order_id` BIGINT DEFAULT NULL COMMENT '关联订单 ID',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

-- 16. 积分规则表
CREATE TABLE IF NOT EXISTS `point_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_key` VARCHAR(50) NOT NULL COMMENT '规则标识',
  `rule_value` INT NOT NULL COMMENT '规则值',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '规则描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则表';

-- 17. 用户积分表
CREATE TABLE IF NOT EXISTS `user_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `total_points` INT NOT NULL DEFAULT 0 COMMENT '累计积分',
  `available_points` INT NOT NULL DEFAULT 0 COMMENT '可用积分',
  `used_points` INT NOT NULL DEFAULT 0 COMMENT '已用积分',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

-- 初始化数据
INSERT IGNORE INTO `coupon` (`id`, `name`, `type`, `discount_value`, `min_amount`, `total_count`, `per_limit`, `start_time`, `end_time`, `status`) VALUES
(1, '新人满减券', 1, 5.00, 30.00, 100, 1, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1),
(2, '全场8折券', 2, 0.80, 50.00, 50, 1, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1),
(3, '满100减20', 1, 20.00, 100.00, 200, 2, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1);

INSERT IGNORE INTO `flash_sale` (`id`, `product_id`, `flash_price`, `flash_stock`, `per_limit`, `start_time`, `end_time`, `status`) VALUES
(1, 1, 0.99, 50, 2, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1),
(2, 4, 29.90, 20, 1, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1),
(3, 15, 49.90, 15, 1, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1);

INSERT IGNORE INTO `group_buy` (`id`, `product_id`, `group_price`, `min_group_size`, `max_group_size`) VALUES
(1, 3, 19.90, 3, 10),
(2, 7, 128.00, 2, 5),
(3, 16, 22.00, 2, 8);

INSERT IGNORE INTO `point_rule` (`rule_key`, `rule_value`, `description`) VALUES
('order_points_rate', 10, '每消费1元获得10积分'),
('sign_in_points', 5, '每日签到获得5积分'),
('min_redeem_points', 100, '最低兑换积分门槛'),
('points_to_money_rate', 100, '100积分抵扣1元');

INSERT IGNORE INTO `user_point` (`user_id`, `total_points`, `available_points`, `used_points`) VALUES
(2, 500, 500, 0),
(3, 200, 200, 0);
