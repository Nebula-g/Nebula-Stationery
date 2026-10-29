-- ============================================
-- 星云商城 功能7 增量建表
-- 短信验证码：user.phone
-- 物流轨迹：logistics / logistics_track
-- ============================================

USE `nebula-store`;

-- -------------------------------------------
-- 用户表扩展：手机号（短信验证码登录用）
-- -------------------------------------------
ALTER TABLE `user`
  ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `role`,
  ADD UNIQUE KEY `uk_phone` (`phone`);

-- -------------------------------------------
-- 物流单（logistics）
-- 一个订单最多一条物流单
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `logistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单 ID',
  `company_code` VARCHAR(20) NOT NULL COMMENT '快递公司编码（SF/YTO/ZTO/JD）',
  `company_name` VARCHAR(50) NOT NULL COMMENT '快递公司名称',
  `tracking_no` VARCHAR(50) NOT NULL COMMENT '运单号',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收件人手机号（真实快递查询需要）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-运输中，1-已签收',
  `shipped_at` DATETIME NOT NULL COMMENT '发货时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_tracking_no` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流单表';

-- -------------------------------------------
-- 物流轨迹（logistics_track）
-- 查询快递接口后落库，接口挂了也能看历史轨迹
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `logistics_track` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `logistics_id` BIGINT NOT NULL COMMENT '物流单 ID',
  `track_time` DATETIME NOT NULL COMMENT '轨迹时间',
  `description` VARCHAR(255) NOT NULL COMMENT '轨迹描述',
  `location` VARCHAR(50) DEFAULT NULL COMMENT '所在城市/网点',
  PRIMARY KEY (`id`),
  KEY `idx_logistics_id` (`logistics_id`, `track_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流轨迹表';
