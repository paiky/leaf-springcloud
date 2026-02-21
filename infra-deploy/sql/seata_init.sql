-- 1. 初始化订单数据库
CREATE DATABASE IF NOT EXISTS `leaf_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `leaf_order`;

-- 创建订单表
CREATE TABLE IF NOT EXISTS `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `count` int(11) DEFAULT '0',
  `money` decimal(10,2) DEFAULT '0.00',
  `status` int(11) DEFAULT '0' COMMENT '订单状态：0:创建中; 1:已完结',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 为 leaf_order 创建 Seata AT 模式必须的 undo_log 表
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2. 切入已存在的用户数据库
USE `leaf_user`;

-- 为用户表加入账户金额的概念，供事务分布式回滚演示 (忽略如果已经存在)
ALTER TABLE `t_user` ADD COLUMN `balance` decimal(10,2) DEFAULT '1000.00' COMMENT '账户余额';

-- 为 leaf_user 同样创建 Seata 的 undo_log 表 (这在每个参与事务的库中都是必须的！)
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
