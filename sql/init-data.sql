-- ============================================
-- 星云商城（Nebula Stationery）初始化数据
-- 数据库：nebula-store
-- 创建时间：2026-10-29
-- ============================================

USE `nebula-store`;

-- -------------------------------------------
-- 1. 用户数据
-- 密码统一为：123456（BCrypt 加密后）
-- BCrypt hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- -------------------------------------------
INSERT INTO `user` (`id`, `username`, `password`, `role`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1),
(2, 'user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 0),
(3, 'user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 0);

-- -------------------------------------------
-- 2. 分类数据
-- 一级分类 3 个，每个下面 2 个二级分类
-- -------------------------------------------
INSERT INTO `category` (`id`, `name`, `parent_id`, `sort_order`, `image_url`) VALUES
-- 一级分类
(1, '书写工具', NULL, 1, '/images/categories/cat-writing-tools.png'),
(2, '纸本文具', NULL, 2, '/images/categories/cat-paper-stationery.png'),
(3, '办公收纳', NULL, 3, '/images/categories/cat-office-storage.png'),
(4, '美术用品', NULL, 4, '/images/categories/cat-art-supplies.png'),
-- 二级分类
(10, '中性笔', 1, 1, NULL),
(11, '钢笔', 1, 2, NULL),
(12, '笔记本', 2, 1, NULL),
(13, '便签纸', 2, 2, NULL),
(14, '文件夹', 3, 1, NULL),
(15, '笔袋', 3, 2, NULL),
(16, '彩铅', 4, 1, NULL),
(17, '水彩笔', 4, 2, NULL);

-- -------------------------------------------
-- 3. 商品数据
-- 共 14 件商品：12 件上架 + 2 件下架，其中 1 件库存为 0
-- -------------------------------------------
INSERT INTO `product` (`id`, `name`, `price`, `stock`, `category_id`, `description`, `image_url`, `status`) VALUES
-- 书写工具 - 中性笔
(1, '晨光中性笔 0.5mm 黑色', 2.50, 500, 10, '经典款中性笔，书写流畅，适合日常书写和考试使用。', '/images/products/pen-black-gel.png', 1),
(2, '晨光中性笔 0.5mm 蓝色', 2.50, 300, 10, '蓝色墨水，颜色鲜艳，适合做笔记和标记。', '/images/products/pen-blue-gel.png', 1),
(3, '得力中性笔套装 12 支装', 25.00, 200, 10, '12 支装经济套装，多色可选，学生党必备。', '/images/products/pen-set-12pack.png', 1),
-- 书写工具 - 钢笔
(4, '英雄钢笔 经典款', 58.00, 100, 11, '国产经典钢笔，笔尖顺滑，适合练字和日常书写。', '/images/products/pen-fountain-classic.png', 1),
(5, '凌美 Safari 钢笔', 128.00, 50, 11, '德国品牌，设计简约，手感舒适，送礼自用两相宜。', '/images/products/pen-fountain-safari.png', 1),
-- 纸本文具 - 笔记本
(6, '国誉活页笔记本 A5', 35.00, 150, 12, '日本品牌，纸质优良，活页设计方便整理。', '/images/products/notebook-looseleaf-a5.png', 1),
(7, 'Moleskine 经典笔记本', 168.00, 80, 12, '意大利品牌，文艺青年最爱，适合手帐和创作。', '/images/products/notebook-classic-black.png', 1),
(8, '得力线圈笔记本 B5', 12.00, 400, 12, '经济实惠，线圈装订不易掉页，学生常用。', '/images/products/notebook-coil-b5.png', 1),
-- 纸本文具 - 便签纸
(9, '3M 便利贴 彩色混装', 15.00, 250, 13, '多色混装，粘性强不留痕，办公学习必备。', '/images/products/sticky-notes-colorful.png', 1),
(10, '国誉便签本 方格', 18.00, 180, 13, '方格设计，适合做思维导图和笔记整理。', '/images/products/sticky-notes-grid.png', 1),
-- 办公收纳 - 文件夹
(11, '得力文件夹 A4 透明', 8.00, 300, 14, '透明设计方便查找，A4 尺寸通用。', '/images/products/folder-a4-transparent.png', 1),
(12, '国誉风琴包 12 格', 45.00, 120, 14, '12 格分类收纳，试卷文件整理神器。', '/images/products/folder-accordion-12slot.png', 1),
-- 办公收纳 - 笔袋（下架商品）
(13, '晨光笔袋 简约款', 22.00, 0, 15, '简约设计，容量大，多色可选。', '/images/products/pencilcase-simple.png', 0),
(14, '国誉笔袋 立体款', 38.00, 90, 15, '立体设计，拿取方便，日系简约风格。', '/images/products/pencilcase-3d-standup.png', 0),
-- 美术用品 - 彩铅
(15, '辉柏嘉彩铅 48 色', 89.00, 60, 16, '德国品牌，色彩鲜艳，适合绘画和涂色。', '/images/products/colored-pencils-48.png', 1),
-- 美术用品 - 水彩笔
(16, '晨光水彩笔 24 色', 32.00, 100, 17, '可水洗设计，儿童绘画首选，颜色丰富。', '/images/products/watercolor-pens-24.png', 1);

-- -------------------------------------------
-- 4. 订单数据
-- 5 种状态各 1 条
-- -------------------------------------------

-- 订单 1：待支付（user1 创建）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `created_at`) VALUES
(1, '202610291000001', 2, 30.00, 0, '2026-10-29 10:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal`) VALUES
(1, 1, 1, '晨光中性笔 0.5mm 黑色', 2.50, 4, 10.00),
(2, 1, 9, '3M 便利贴 彩色混装', 15.00, 1, 15.00),
(3, 1, 11, '得力文件夹 A4 透明', 8.00, 1, 8.00);

-- 订单 2：已支付（user1 创建）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `created_at`) VALUES
(2, '202610281500001', 2, 163.00, 1, '2026-10-28 15:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal`) VALUES
(4, 2, 4, '英雄钢笔 经典款', 58.00, 1, 58.00),
(5, 2, 6, '国誉活页笔记本 A5', 35.00, 2, 70.00),
(6, 2, 10, '国誉便签本 方格', 18.00, 1, 18.00),
(7, 2, 16, '晨光水彩笔 24 色', 32.00, 1, 32.00);

-- 订单 3：已发货（user2 创建）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `created_at`) VALUES
(3, '202610270900001', 3, 128.00, 2, '2026-10-27 09:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal`) VALUES
(8, 3, 5, '凌美 Safari 钢笔', 128.00, 1, 128.00);

-- 订单 4：已完成（user2 创建）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `created_at`) VALUES
(4, '202610261400001', 3, 114.00, 3, '2026-10-26 14:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal`) VALUES
(9, 4, 15, '辉柏嘉彩铅 48 色', 89.00, 1, 89.00),
(10, 4, 3, '得力中性笔套装 12 支装', 25.00, 1, 25.00);

-- 订单 5：已取消（user1 创建）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `created_at`) VALUES
(5, '202610251100001', 2, 45.00, 4, '2026-10-25 11:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal`) VALUES
(11, 5, 12, '国誉风琴包 12 格', 45.00, 1, 45.00);

-- -------------------------------------------
-- 5. 评价数据
-- 已完成订单（订单 4）的商品评价
-- -------------------------------------------
INSERT INTO `review` (`id`, `user_id`, `product_id`, `order_id`, `rating`, `content`, `created_at`) VALUES
(1, 3, 15, 4, 5, '颜色很正，画画效果很好，孩子很喜欢！', '2026-10-27 10:00:00'),
(2, 3, 3, 4, 4, '性价比不错，12 支装很实惠，就是笔杆有点细。', '2026-10-27 10:05:00');

-- -------------------------------------------
-- 6. 购物车数据
-- user1 的购物车
-- -------------------------------------------
INSERT INTO `cart_item` (`id`, `user_id`, `product_id`, `quantity`) VALUES
(1, 2, 1, 2),
(2, 2, 6, 1),
(3, 2, 9, 3);

-- -------------------------------------------
-- 7. 优惠券数据
-- -------------------------------------------
INSERT INTO `coupon` (`id`, `name`, `type`, `discount_value`, `min_amount`, `total_count`, `per_limit`, `start_time`, `end_time`, `status`) VALUES
(1, '新人满减券', 1, 5.00, 30.00, 100, 1, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1),
(2, '全场8折券', 2, 0.80, 50.00, 50, 1, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1),
(3, '满100减20', 1, 20.00, 100.00, 200, 2, '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1);

-- -------------------------------------------
-- 8. 秒杀商品数据
-- -------------------------------------------
INSERT INTO `flash_sale` (`id`, `product_id`, `flash_price`, `flash_stock`, `per_limit`, `start_time`, `end_time`, `status`) VALUES
(1, 1, 0.99, 50, 2, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1),
(2, 4, 29.90, 20, 1, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1),
(3, 15, 49.90, 15, 1, '2026-10-29 00:00:00', '2026-12-31 23:59:59', 1);

-- -------------------------------------------
-- 9. 拼团商品数据
-- -------------------------------------------
INSERT INTO `group_buy` (`id`, `product_id`, `group_price`, `min_group_size`, `max_group_size`) VALUES
(1, 3, 19.90, 3, 10),
(2, 7, 128.00, 2, 5),
(3, 16, 22.00, 2, 8);

-- -------------------------------------------
-- 10. 积分规则数据
-- -------------------------------------------
INSERT INTO `point_rule` (`rule_key`, `rule_value`, `description`) VALUES
('order_points_rate', 10, '每消费1元获得10积分'),
('sign_in_points', 5, '每日签到获得5积分'),
('min_redeem_points', 100, '最低兑换积分门槛'),
('points_to_money_rate', 100, '100积分抵扣1元');

-- -------------------------------------------
-- 11. 用户积分数据
-- -------------------------------------------
INSERT INTO `user_point` (`user_id`, `total_points`, `available_points`, `used_points`) VALUES
(2, 500, 500, 0),
(3, 200, 200, 0);

-- ============================================
-- 初始化数据完成
-- ============================================

-- 数据说明：
-- 用户：admin(管理员), user1(普通用户), user2(普通用户)，密码均为 123456
-- 分类：4 个一级分类，8 个二级分类
-- 商品：16 件（14 件上架 + 2 件下架，其中 1 件库存为 0）
-- 订单：5 种状态各 1 条
-- 评价：已完成订单的 2 件商品各有 1 条评价
-- 购物车：user1 有 3 件商品
