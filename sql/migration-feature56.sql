-- ============================================
-- 星云商城 功能5+6 增量建表
-- 功能5：评价图片、追评、点赞、敏感词
-- 功能6：搜索联想、销量统计（依赖 sold_count）、导出 Excel
-- ============================================

USE `nebula-store`;

-- -------------------------------------------
-- 22. 评价表扩展：图片 + 点赞数
-- -------------------------------------------
ALTER TABLE `review`
  ADD COLUMN `image_urls` VARCHAR(1000) DEFAULT NULL COMMENT '评价图片（JSON数组）' AFTER `content`,
  ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `image_urls`;

-- -------------------------------------------
-- 23. 追评表（review_append）
-- 一条评价可以有 0 或 1 条追评
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `review_append` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '原评价 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `content` VARCHAR(500) NOT NULL COMMENT '追评内容',
  `image_urls` VARCHAR(1000) DEFAULT NULL COMMENT '追评图片（JSON数组）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '追评时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_id` (`review_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价追评表';

-- -------------------------------------------
-- 24. 评价点赞表（review_like）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `review_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id` BIGINT NOT NULL COMMENT '评价 ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_user` (`review_id`, `user_id`),
  KEY `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价点赞表';

-- -------------------------------------------
-- 25. 敏感词表（sensitive_word）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `category` VARCHAR(20) NOT NULL DEFAULT '通用' COMMENT '分类：辱骂/广告/违禁/通用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

INSERT INTO `sensitive_word` (`word`, `category`) VALUES
  ('傻逼', '辱骂'),
  ('混蛋', '辱骂'),
  ('王八蛋', '辱骂'),
  ('去死', '辱骂'),
  ('垃圾东西', '辱骂'),
  ('代刷', '广告'),
  ('加微信', '广告'),
  ('加QQ', '广告'),
  ('微商', '广告'),
  ('办证', '违禁'),
  ('枪支', '违禁'),
  ('毒品', '违禁'),
  ('赌博', '违禁'),
  ('诈骗', '违禁')
ON DUPLICATE KEY UPDATE `category` = VALUES(`category`);

-- -------------------------------------------
-- 26. 商品表扩展：销量字段（销量统计用）
-- -------------------------------------------
ALTER TABLE `product`
  ADD COLUMN `sold_count` INT NOT NULL DEFAULT 0 COMMENT '累计销量' AFTER `stock`;

-- 搜索联想用索引
ALTER TABLE `product` ADD INDEX `idx_name` (`name`);
