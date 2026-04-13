/*
 Navicat MySQL Data Transfer

 Source Server         : MySql
 Source Server Type    : MySQL
 Source Server Version : 50729
 Source Host           : localhost:3306
 Source Schema         : cloudland

 Target Server Type    : MySQL
 Target Server Version : 50729
 File Encoding         : 65001

 Date: 27/05/2024 18:29:09

 Fixed:
 1. `user.id` 改为单列主键，满足外键引用条件
 2. 按依赖顺序重排建表和插入顺序，便于直接导入
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Drop tables
-- ----------------------------
DROP TABLE IF EXISTS `cloudland_file`;
DROP TABLE IF EXISTS `order2`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `trolley`;
DROP TABLE IF EXISTS `land`;
DROP TABLE IF EXISTS `land_type`;
DROP TABLE IF EXISTS `msg_send`;
DROP TABLE IF EXISTS `msg`;
DROP TABLE IF EXISTS `user`;

-- ----------------------------
-- Table structure for user
-- ----------------------------
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '0',
  `password` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `age` int(11) NOT NULL,
  `phone` char(11) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `address` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `img` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT 'basic.png',
  `power` int(1) NOT NULL DEFAULT 0,
  `debt` double(10, 2) NOT NULL DEFAULT 0.00,
  `detailed_address` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `status` int(1) NOT NULL DEFAULT 1,
  `mail` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_phone` (`phone`) USING BTREE,
  UNIQUE KEY `uk_user_mail` (`mail`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '暂无', '1', 19, '112442142', '120000,120100,120102', 'userIcon_6523b5b5-c89c-41bb-8f9b-fdd4403f5e3c.jpg', 2, 1.00, '法撒旦哇哇', 0, '0');
INSERT INTO `user` VALUES (17, 'Banana大魔仙', '$2a$10$26/M4738gUWQXNaX1adS1.06xTT43SaLjAeQZVZi1Esw0F4FaAaF2', 32, '19881695091', '150000,150400,150404', 'userIcon_bd51155b-4e47-4b49-8ab7-19362fc1ae8e.png', 1, 0.00, '撒旦萨达', 1, '780602815@qq.com');
INSERT INTO `user` VALUES (26, '左厚博', '$2a$10$vBxmgHJ6u3N4qpL23V7tD.oyFse/LXU9XeGbteNTj333d2usx7xN2', 23, '18140213284', '510000,510100,510101', 'userIcon_1b2e1df1-f6be-455f-9d95-b036f12513a9.jpg', 2, 0.00, '的发射点犯得上房贷首付666', 0, '1909173237@qq.com');
INSERT INTO `user` VALUES (27, '西毒', '$2a$10$Y.U5Gm96SaGPnG9REE.AkOgky4jexXdOwYIjr1a9O6AOJdrD3oP3K', 12, '19881695097', '120000,120100,120102', 'userIcon_59d8eea9-e6bd-4283-ba8b-7c62a7e8e8a1.jpg', 2, 0.00, 'asdsdsad', 1, '1');
INSERT INTO `user` VALUES (29, '东邪', '$2a$10$qX3iTIQbripttFK1F7J.uuhzmycyeoRFEpNF9a0Bft.kozqBaY5Oq', 19, '18140213286', '120000,120100,120103', 'userIcon_e6a53713-d23a-4691-84e3-9069c4b80840.jpg', 2, 0.00, 'csdasd', 1, '780602817@qq.com');
INSERT INTO `user` VALUES (30, '南帝', '$2a$10$iKXDbxDiWqgrwqY20p3or.t4k4buTJC7fi4nkPhBvS0Rq9ZqadgXa', 98, '13982411165', '110000,110100,110101', 'userIcon_c71253b8-83c5-4e2c-9761-99c5c5dc8395.jpg', 0, 0.00, '88888888', 0, '2543635236@qq.com');
INSERT INTO `user` VALUES (31, '中神通', '$2a$10$NqKdLq0/og7tkzk0BEGMnedQVIp5vte5vxgnwl5g3bdqkchWcopiW', 32, '19881695096', '150000,150300,150303', 'userIcon_4eb249e4-e394-4ff2-8ef7-6d37b78c4adf.jpg', 2, 0.00, '撒大苏打21', 1, '780602816@qq.com');
INSERT INTO `user` VALUES (32, '左厚博', '$2a$10$kMqluEEsjt2zWH3vKyHtMOCH5YPsOjLN5/llQOqaoYYgYrupzjCZW', 18, '18140213287', '510000,510100,510117', 'userIcon_5d13c4b7-a044-43ba-a9b8-4207d95d192d.gif', 2, 0.00, '观澜国际1期', 1, '676104035@qq.com');
INSERT INTO `user` VALUES (33, '12', '$2a$10$UonPVjHxnz8ugv6SMrg0UOsiYoL6gyaBBFI2bYRdstBNWUjZ04Svi', 19, '12345678910', '110000,110100,110102', 'userIcon_3ac77c17-888f-4dc9-91ed-03bee94d6e80.jpg', 2, 0.00, 'xnknla', 1, '676104031@qq.com');

-- ----------------------------
-- Table structure for land_type
-- ----------------------------
CREATE TABLE `land_type` (
  `id` int(11) NOT NULL,
  `type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '类型名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of land_type
-- ----------------------------
INSERT INTO `land_type` VALUES (1, '农用地', '用于农业生产的土地，包括耕地、林地、草地、农田水利用地、养殖水面等');
INSERT INTO `land_type` VALUES (2, '建设用地', '用于建造建筑物、构筑物的土地，包括城乡住宅和公共设施用地、工矿用地、交通水利设施用地、旅游用地、军事设施用地等');
INSERT INTO `land_type` VALUES (3, '商业用地', '用于开展商业、旅游、娱乐活动所占用的场所，如商店、粮店、饮食店、公园、游乐场、影剧院和体育场馆等');
INSERT INTO `land_type` VALUES (4, '公共管理与公共服务用地', '用于党政机关、群众团体、军队警察、社会团体等所占用的土地，以及教育、科研、文化、卫生、体育、通信、公共绿地的土地');
INSERT INTO `land_type` VALUES (5, '水域及水利设施用地', '用于工农业生产、人民生活和环境卫生等所占用的河流、湖泊、水库、渠道等土地，包括河流、湖泊水面、内陆水域等');
INSERT INTO `land_type` VALUES (6, '其他土地', '无');

-- ----------------------------
-- Table structure for land
-- ----------------------------
CREATE TABLE `land` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `land_name` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '测试数据' COMMENT '名称',
  `land_type` int(10) NOT NULL DEFAULT 1 COMMENT '类型',
  `description` varchar(500) CHARACTER SET utf8 COLLATE utf8_german2_ci NOT NULL DEFAULT '无' COMMENT '描述',
  `address` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '位置',
  `ordered` int(5) NOT NULL DEFAULT 100 COMMENT '排序权重',
  `price` double(10, 2) NOT NULL DEFAULT 0.00 COMMENT '价格/平方米/天',
  `a_id` int(10) NOT NULL COMMENT '所有者ID',
  `status` int(5) NOT NULL DEFAULT 0 COMMENT '状态',
  `area` double(25, 2) NOT NULL DEFAULT 0.00 COMMENT '面积（平方米）',
  `employee_id` int(11) NULL DEFAULT NULL COMMENT '土地代理人',
  `detailed_address` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '详细地址',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_land_type` (`land_type`) USING BTREE,
  KEY `idx_land_address` (`address`) USING BTREE,
  KEY `idx_land_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_land_a_id` (`a_id`) USING BTREE,
  CONSTRAINT `fk_land_land_type` FOREIGN KEY (`land_type`) REFERENCES `land_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_land_employee` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_land_upload_user` FOREIGN KEY (`a_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 219 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of land
-- ----------------------------
INSERT INTO `land` VALUES (213, '测试数据1', 1, '该地处于温带', '510000,510700,510722', 98, 23.00, 32, 0, 100.00, 26, '大同');
INSERT INTO `land` VALUES (214, '测试数据2', 5, '该地富饶，具有完善的灌溉系统', '510000,510700,510701', 100, 43.00, 32, 0, 145.00, 26, '五县大唐');
INSERT INTO `land` VALUES (215, '测试数据3', 5, '暂时没有数据', '510000,510700,510725', 90, 45.00, 32, 1, 35.00, 32, '成都金堂');
INSERT INTO `land` VALUES (216, '测试数据4', 5, '1111111111', '510000,510700,510705', 100, 1.00, 32, 1, 1.00, 1, '11111111');
INSERT INTO `land` VALUES (217, '测试数据5', 4, '暂时没有', '510000,510700,510704', 100, 15.00, 32, 1, 100.00, 29, '暂时没有111');
INSERT INTO `land` VALUES (218, '123', 5, '111111111111', '510000,510700,510705', 100, 12.00, 32, 1, 12.00, 29, '111111');

-- ----------------------------
-- Table structure for cloudland_file
-- ----------------------------
CREATE TABLE `cloudland_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `land_id` int(11) NOT NULL,
  `type` int(1) NOT NULL COMMENT '0为图片文件,1为用地文件',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cloudland_file_land_id` (`land_id`) USING BTREE,
  CONSTRAINT `fk_cloudland_file_land` FOREIGN KEY (`land_id`) REFERENCES `land` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 519 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cloudland_file
-- ----------------------------
INSERT INTO `cloudland_file` VALUES (466, '云用地_214.zip', 214, 1);
INSERT INTO `cloudland_file` VALUES (468, '云用地_215.zip', 215, 1);
INSERT INTO `cloudland_file` VALUES (469, 'Img_215_0.jpg', 215, 0);
INSERT INTO `cloudland_file` VALUES (470, '云用地_216.zip', 216, 1);
INSERT INTO `cloudland_file` VALUES (471, 'Img_216_0.png', 216, 0);
INSERT INTO `cloudland_file` VALUES (481, 'Img_214_0.jpg', 214, 0);
INSERT INTO `cloudland_file` VALUES (505, '云用地_213609674.zip', 213, 1);
INSERT INTO `cloudland_file` VALUES (507, 'Img_213_409416.jpg', 213, 0);
INSERT INTO `cloudland_file` VALUES (508, 'Img_213_517598.jpg', 213, 0);
INSERT INTO `cloudland_file` VALUES (509, 'Img_213_575774.jpg', 213, 0);
INSERT INTO `cloudland_file` VALUES (513, '云用地_217593219.zip', 217, 1);
INSERT INTO `cloudland_file` VALUES (514, 'Img_217_683576.jpg', 217, 0);
INSERT INTO `cloudland_file` VALUES (515, 'Img_217_564264.jpg', 217, 0);
INSERT INTO `cloudland_file` VALUES (516, '云用地_218357373.zip', 218, 1);
INSERT INTO `cloudland_file` VALUES (517, 'Img_218_353960.jpg', 218, 0);
INSERT INTO `cloudland_file` VALUES (518, 'Img_218_203627.png', 218, 0);

-- ----------------------------
-- Table structure for order2
-- ----------------------------
CREATE TABLE `order2` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `p_id` int(11) NULL DEFAULT NULL,
  `num` int(11) NULL DEFAULT NULL,
  `u_id` int(11) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `pay_time` datetime NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT NULL,
  `del` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order2_u_id` (`u_id`) USING BTREE,
  CONSTRAINT `fk_order2_user` FOREIGN KEY (`u_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order2
-- ----------------------------
INSERT INTO `order2` VALUES (58, 214, -1, 32, '2024-04-11 22:21:47', '2024-04-12 02:14:30', 1, 0);
INSERT INTO `order2` VALUES (59, 215, -1, 32, '2024-04-11 22:23:54', '2024-04-11 22:27:39', 1, 0);
INSERT INTO `order2` VALUES (60, 3, 2, 27, '2024-04-05 23:11:42', '2024-04-12 02:14:43', 1, 0);
INSERT INTO `order2` VALUES (61, 3, 3, 26, '2024-04-21 23:12:49', '2024-04-11 23:12:51', 1, 0);
INSERT INTO `order2` VALUES (62, 3, 1, 32, '2024-04-11 23:13:13', '2024-04-12 23:13:15', 1, 0);
INSERT INTO `order2` VALUES (64, 5, 5, 32, '2024-04-11 23:14:11', '2024-04-17 22:31:24', 2, 0);
INSERT INTO `order2` VALUES (65, 1, 1, 32, '2024-04-12 11:47:50', '2024-04-20 10:57:30', 1, 0);
INSERT INTO `order2` VALUES (66, 214, -1, 32, '2024-04-17 22:36:23', '2024-04-17 22:36:36', 1, 0);
INSERT INTO `order2` VALUES (67, 1, 3, 32, '2024-04-17 22:37:15', '2024-04-17 22:37:27', 1, 0);
INSERT INTO `order2` VALUES (68, 1, 2, 17, '2024-04-18 22:57:09', '2024-04-19 22:57:14', 1, 0);
INSERT INTO `order2` VALUES (69, 214, -1, 32, '2024-04-20 10:57:15', '2024-04-20 10:57:30', 1, 0);

-- ----------------------------
-- Table structure for product
-- ----------------------------
CREATE TABLE `product` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_name` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `ordered` int(255) NOT NULL,
  `price` double(10, 2) NOT NULL,
  `status` int(1) NOT NULL DEFAULT 0,
  `a_id` int(10) NOT NULL DEFAULT 1,
  `num` double(10, 2) NOT NULL,
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_a_id` (`a_id`) USING BTREE,
  CONSTRAINT `fk_product_user` FOREIGN KEY (`a_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, '苹果', '苹果营养价值高', 3, 15.00, 1, 32, 18.00, 'product_ca09eddf-c81b-4846-84fc-d80c2cd39d65.jpg');
INSERT INTO `product` VALUES (3, '荔枝', '又小又难吃', 9, 8.00, 0, 32, 8.00, 'product_e2f028cd-6de3-44f1-b2bf-5c07831ccab0.jpg');
INSERT INTO `product` VALUES (4, '橘子', '12334', 98, 12.00, 1, 1, 18.00, 'product_12d68b2c-3503-45fd-9373-e22d433720f0.jpg');
INSERT INTO `product` VALUES (5, '西瓜', '2121', 98, 23.00, 1, 1, 22.00, 'product_30d4412f-89b7-4e39-936c-9c336d222831.jpg');
INSERT INTO `product` VALUES (7, '芹菜', '12', 100, 122.00, 0, 31, 323.00, 'product_ff363bed-2bb7-4181-bcee-86117b9e9791.jpg');
INSERT INTO `product` VALUES (8, '向日葵222', '暂时没有', 100, 15.00, 0, 32, 12.00, 'product_ba7b2551-94ea-4320-ae2e-569f4b192e9a.png');

-- ----------------------------
-- Table structure for trolley
-- ----------------------------
CREATE TABLE `trolley` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `p_id` int(11) NULL DEFAULT NULL,
  `num` int(11) NULL DEFAULT NULL,
  `u_id` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_trolley_u_id` (`u_id`) USING BTREE,
  KEY `idx_trolley_p_id` (`p_id`) USING BTREE,
  CONSTRAINT `fk_trolley_user` FOREIGN KEY (`u_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of trolley
-- ----------------------------
INSERT INTO `trolley` VALUES (61, 213, -1, 32);
INSERT INTO `trolley` VALUES (62, 1, 1, 32);
INSERT INTO `trolley` VALUES (63, 213, -1, 32);

-- ----------------------------
-- Table structure for msg
-- ----------------------------
CREATE TABLE `msg` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `send_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of msg
-- ----------------------------
INSERT INTO `msg` VALUES (1, 'wqwe', '676104035@qq.com', '18140213287', 'wewqec', '2024-02-23 21:21:25');
INSERT INTO `msg` VALUES (24, 'weqwe', '676104035@qq.com', '18140213287', 'wqeweewqew', '2024-02-23 22:20:44');
INSERT INTO `msg` VALUES (25, 'dxxx', '676104035@qq.com', '18140213287', 'ffdfs', '2024-03-20 23:35:40');
INSERT INTO `msg` VALUES (26, '仍然', '676104035@qq.com', '18140213287', '十大', '2024-04-10 10:26:18');
INSERT INTO `msg` VALUES (27, '333', '676104035@qq.com', '18140213287', 'sdasdsd', '2024-04-20 10:26:42');
INSERT INTO `msg` VALUES (28, 'wqwe', '676104035@qq.com', '18140213287', 'weweqwe', '2024-04-20 10:26:56');
INSERT INTO `msg` VALUES (29, '212', '676104035@qq.com', '18140213287', 'xcsada', '2024-04-20 10:27:24');
INSERT INTO `msg` VALUES (30, '231231', '676104035@qq.com', '18140213287', '12312', '2024-04-20 10:27:56');
INSERT INTO `msg` VALUES (31, 'aaaa', '676104035@qq.com', '18140213287', 'sdasda', '2024-04-20 10:28:12');
INSERT INTO `msg` VALUES (34, '123', '676104035@qq.com', '18140213287', '1111111', '2024-04-20 10:54:52');

-- ----------------------------
-- Table structure for msg_send
-- ----------------------------
CREATE TABLE `msg_send` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`, `mail`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of msg_send
-- ----------------------------
INSERT INTO `msg_send` VALUES (1, '676104035@qq.com');

SET FOREIGN_KEY_CHECKS = 1;
