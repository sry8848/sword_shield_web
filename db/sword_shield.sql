/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成果ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '成果描述',
  `file_url` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联文件链接',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成果展示表';

DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动标题',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '活动简介',
  `cover_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图URL',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '活动地点',
  `speaker` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主讲嘉宾',
  `live_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '直播链接',
  `replay_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回放链接',
  `max_registrations` int(11) DEFAULT NULL COMMENT '报名人数上限',
  `current_registrations` int(11) DEFAULT '0' COMMENT '当前报名人数',
  `views` int(11) DEFAULT '0' COMMENT '浏览量',
  `heat` int(11) DEFAULT '0' COMMENT '热度值',
  `status` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_heat` (`heat`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

DROP TABLE IF EXISTS `activity_registration`;
CREATE TABLE `activity_registration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报名记录ID',
  `activity_id` bigint(20) NOT NULL COMMENT '活动ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `channel` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'web' COMMENT '报名渠道（例如 web/app/onsite）',
  `registered_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '管理员用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(加密存储)',
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `status` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员用户表';

DROP TABLE IF EXISTS `asset_file`;
CREATE TABLE `asset_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `object_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象存储对象 Key',
  `bucket` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Bucket 名称',
  `content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME 类型',
  `size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '归属用户ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_object_name` (`object_name`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件资产表';

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: activity, article, achievement等',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

DROP TABLE IF EXISTS `content_article`;
CREATE TABLE `content_article` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '文章摘要',
  `body` longtext COLLATE utf8mb4_unicode_ci COMMENT '文章正文',
  `cover_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图URL',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '通用' COMMENT '分类: 技术, 安全, 行业, 通知等',
  `author` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作者',
  `views` int(11) DEFAULT '0' COMMENT '浏览量',
  `status` int(11) NOT NULL,
  `published_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `live_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `replay_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资讯文章表';

DROP TABLE IF EXISTS `exhibitor`;
CREATE TABLE `exhibitor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '展商ID',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '展商名称',
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Logo URL',
  `introduction` text COLLATE utf8mb4_unicode_ci COMMENT '展商介绍',
  `website` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '官网地址',
  `brand` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `video_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '宣传视频URL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='展商表';

DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: activity, article, comment等',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录表';

DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '资料ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料标题',
  `speaker` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主讲人',
  `object_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO对象名称',
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `size` bigint(20) DEFAULT '0' COMMENT '文件大小(字节)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料表';

DROP TABLE IF EXISTS `media_asset`;
CREATE TABLE `media_asset` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '媒体资源ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `summary` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摘要/说明',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型: image, video',
  `object_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO对象名称',
  `cover_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图URL',
  `article_id` bigint(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体资源表';

DROP TABLE IF EXISTS `poster_job`;
CREATE TABLE `poster_job` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务ID',
  `subject` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务归属主体(用户subject)',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '海报标题',
  `subtitle` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '海报副标题',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态: PENDING/PROCESSING/SUCCESS/FAILED',
  `result_url` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果URL(成功时)',
  `error_message` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息(失败时)',
  `dispatch_attempts` int(11) NOT NULL DEFAULT '0' COMMENT '投递次数',
  `dispatch_last_error` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近一次投递错误',
  `dispatch_last_attempt_at` timestamp NULL DEFAULT NULL COMMENT '最近一次投递时间',
  `dispatch_next_retry_at` timestamp NULL DEFAULT NULL COMMENT '下次允许投递时间(用于重试/锁)',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_due` (`status`,`dispatch_next_retry_at`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海报生成任务表';

DROP TABLE IF EXISTS `points_txn`;
CREATE TABLE `points_txn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变动类型: daily_checkin/activity_checkin 等',
  `points` int(11) DEFAULT NULL COMMENT '变动积分',
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `share_record`;
CREATE TABLE `share_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分享记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: activity, article等',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID',
  `channel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分享渠道: wechat/weibo/link 等',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享记录表';

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(加密存储)',
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

DROP TABLE IF EXISTS `user_checkin`;
CREATE TABLE `user_checkin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '签到日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`checkin_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_checkin_date` (`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到记录表';

DROP TABLE IF EXISTS `activity_checkin`;
CREATE TABLE `activity_checkin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '活动签到记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `activity_id` bigint(20) NOT NULL COMMENT '活动ID',
  `checked_in_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`,`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_checked_in_at` (`checked_in_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动签到记录表';

DROP TABLE IF EXISTS `user_points`;
CREATE TABLE `user_points` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '积分记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `total` int(11) DEFAULT '0' COMMENT '总积分',
  `available` int(11) DEFAULT '0' COMMENT '可用积分',
  `used` int(11) DEFAULT '0' COMMENT '已使用积分',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

INSERT INTO `achievement` (`id`, `title`, `description`, `file_url`) VALUES
(1, '新一代AI安全防护系统', '基于深度学习的智能威胁检测系统,能够实时识别未知威胁,检测准确率达99.5%以上。', 'https://picsum.photos/seed/achievement1/600/400'),
(2, '零信任网络访问(ZTNA)平台', '基于零信任理念的网络访问控制平台,支持细粒度访问控制和持续信任评估。', 'https://picsum.photos/seed/achievement2/600/400'),
(3, '工业互联网安全监测平台', '针对工业控制系统的安全监测和防护平台,支持多种工业协议,实现全方位安全防护。', 'https://picsum.photos/seed/achievement3/600/400'),
(4, '软件供应链安全管理系统', '全生命周期的软件供应链安全管理工具,支持SBOM生成、漏洞扫描和合规检查。', 'https://picsum.photos/seed/achievement4/600/400'),
(5, '云原生安全防护方案', '面向容器和K8s环境的安全防护解决方案,提供镜像扫描、运行时保护等功能。', 'https://picsum.photos/seed/achievement5/600/400'),
(6, '数据安全治理平台', '支持数据发现、分类分级、脱敏、审计的一体化数据安全治理平台。', 'https://picsum.photos/seed/achievement6/600/400'),
(7, '安全运营中心(SOC)解决方案', '集成了SIEM、SOAR、威胁情报的新一代安全运营中心,实现安全事件的自动化响应。', 'https://picsum.photos/seed/achievement7/600/400'),
(8, '移动应用安全检测平台', '针对Android和iOS应用的自动化安全检测工具,支持静态分析和动态测试。', 'https://picsum.photos/seed/achievement8/600/400'),
(9, '区块链安全审计工具', '智能合约安全审计工具,支持多种区块链平台,自动发现合约漏洞。', 'https://picsum.photos/seed/achievement9/600/400'),
(10, '安全态势感知系统', '基于大数据和AI的安全态势感知系统,实现全网安全风险的可视化和预警。', 'https://picsum.photos/seed/achievement10/600/400');

INSERT INTO `activity` (`id`, `title`, `summary`, `cover_url`, `start_time`, `end_time`, `location`, `speaker`, `live_url`, `replay_url`, `max_registrations`, `current_registrations`, `heat`, `status`, `created_at`, `updated_at`) VALUES
(1, '2025西湖论剑·数字安全大会开幕式', '西湖论剑作为国内网络安全领域的顶级盛会,将汇聚政府领导、行业专家、企业代表等各界人士,共同探讨数字安全的未来发展方向。', 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop', '2025-11-10 19:53:32', '2025-11-30 19:53:32', '浙江省杭州市西湖国际会议中心', '院士专家团队', 'https://live.example.com/opening', 'https://live.example.com/ai-security', 1000, 856, 998, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50');
INSERT INTO `activity` (`id`, `title`, `summary`, `cover_url`, `start_time`, `end_time`, `location`, `speaker`, `live_url`, `replay_url`, `max_registrations`, `current_registrations`, `heat`, `status`, `created_at`, `updated_at`) VALUES
(2, '网络安全攻防演练分论坛', '邀请国内顶尖安全团队现场演示最新的攻防技术,探讨实战化安全防护体系建设。', 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&h=450&fit=crop', '2025-05-15 14:00:00', '2025-12-15 17:30:00', '浙江省杭州市西湖国际会议中心A厅', '陈安全、李防护', 'https://live.example.com/attack-defense', 'https://live.example.com/ai-security', 500, 432, 876, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50');
INSERT INTO `activity` (`id`, `title`, `summary`, `cover_url`, `start_time`, `end_time`, `location`, `speaker`, `live_url`, `replay_url`, `max_registrations`, `current_registrations`, `heat`, `status`, `created_at`, `updated_at`) VALUES
(3, '数据安全与隐私保护专题', '聚焦数据安全治理、个人信息保护、数据跨境流动等热点问题,分享最佳实践案例。', 'https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&h=450&fit=crop', '2025-05-16 09:00:00', '2025-05-16 12:00:00', '浙江省杭州市西湖国际会议中心B厅', '王数据、张隐私', 'https://live.example.com/data-security', 'https://replay.example.com/data-security', 600, 523, 765, 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `activity` (`id`, `title`, `summary`, `cover_url`, `start_time`, `end_time`, `location`, `speaker`, `live_url`, `replay_url`, `max_registrations`, `current_registrations`, `heat`, `status`, `created_at`, `updated_at`) VALUES
(4, 'AI与网络安全融合创新论坛', '探讨人工智能技术在网络安全领域的应用,包括AI威胁检测、智能安全运营等前沿话题。', 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=450&fit=crop', '2025-05-16 14:00:00', '2025-05-16 17:00:00', '浙江省杭州市西湖国际会议中心C厅', '刘智能、赵创新', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 400, 387, 892, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(5, '云安全与零信任架构实践', '分享云原生安全、零信任网络架构的设计理念和实施经验,助力企业数字化转型。', 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&h=450&fit=crop', '2025-05-17 09:30:00', '2025-05-17 11:30:00', '浙江省杭州市西湖国际会议中心D厅', '周云计算、吴零信任', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 300, 256, 654, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(6, '工业互联网安全保障体系建设', '针对工业互联网安全面临的挑战,探讨安全保障体系建设路径和关键技术。', 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=800&h=450&fit=crop', '2025-05-17 14:00:00', '2025-05-17 16:30:00', '浙江省杭州市西湖国际会议中心E厅', '郑工业、钱互联', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 350, 198, 543, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(7, '开源软件安全治理论坛', '讨论开源软件供应链安全风险管理、漏洞治理等关键问题。', 'https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=800&h=450&fit=crop', '2025-05-18 10:00:00', '2025-05-18 12:00:00', '浙江省杭州市西湖国际会议中心F厅', '孙开源、李治理', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 250, 167, 432, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(8, '安全人才培养与职业发展', '邀请行业HR和资深安全专家分享人才需求趋势、职业发展路径。', 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&h=450&fit=crop', '2025-05-18 14:30:00', '2025-05-18 16:30:00', '浙江省杭州市西湖国际会议中心G厅', '何人才、林教育', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 400, 312, 721, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(9, '网络安全法律法规与合规实践', '解读最新网络安全法律法规,分享企业合规实践经验。', 'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=800&h=450&fit=crop', '2025-05-19 09:00:00', '2025-05-19 11:00:00', '浙江省杭州市西湖国际会议中心H厅', '高法律、唐合规', 'https://live.example.com/ai-security', 'https://live.example.com/ai-security', 200, 156, 389, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50'),
(10, '西湖论剑·网络安全创新大赛颁奖典礼', '表彰在网络安全技术创新和实践应用中表现突出的团队和个人。', 'https://images.unsplash.com/photo-1559136555-9303baea8ebd?w=800&h=450&fit=crop', '2025-05-19 15:00:00', '2025-05-19 17:00:00', '浙江省杭州市西湖国际会议中心主会场', '组委会', 'https://live.example.com/awards', 'https://live.example.com/ai-security', 801, 745, 956, 1, '2025-11-26 19:53:32', '2025-12-01 19:55:50');

INSERT INTO `activity_registration` (`id`, `activity_id`, `user_id`, `channel`, `registered_at`) VALUES
(1, 1, 2, 'web', '2025-11-26 19:53:32'),
(2, 1, 3, 'app', '2025-11-26 19:53:32'),
(3, 1, 4, 'share_link', '2025-11-26 19:53:32'),
(4, 1, 5, 'web', '2025-11-26 19:53:32'),
(5, 2, 2, 'web', '2025-11-26 19:53:32'),
(6, 2, 4, 'web', '2025-11-26 19:53:32'),
(7, 2, 6, 'app', '2025-11-26 19:53:32'),
(8, 3, 3, 'web', '2025-11-26 19:53:32'),
(9, 3, 5, 'web', '2025-11-26 19:53:32'),
(10, 3, 7, 'share_link', '2025-11-26 19:53:32'),
(11, 4, 2, 'web', '2025-11-26 19:53:32'),
(12, 4, 3, 'web', '2025-11-26 19:53:32'),
(13, 4, 8, 'app', '2025-11-26 19:53:32'),
(14, 5, 4, 'web', '2025-11-26 19:53:32'),
(15, 5, 6, 'web', '2025-11-26 19:53:32'),
(16, 6, 5, 'web', '2025-11-26 19:53:32'),
(17, 7, 7, 'web', '2025-11-26 19:53:32'),
(18, 8, 8, 'web', '2025-11-26 19:53:32'),
(19, 8, 9, 'app', '2025-11-26 19:53:32'),
(20, 9, 6, 'web', '2025-11-26 19:53:32'),
(21, 10, 2, 'web', '2025-11-26 19:53:32'),
(22, 10, 3, 'web', '2025-11-26 19:53:32'),
(23, 10, 4, 'web', '2025-11-26 19:53:32'),
(24, 10, 5, 'web', '2025-11-26 19:53:32'),
(25, 10, 6, 'web', '2025-11-26 19:53:32'),
(26, 1, 11, 'web', '2025-11-26 12:16:37'),
(27, 2, 11, 'web', '2025-11-26 15:55:14'),
(28, 2, 12, 'web', '2025-11-26 16:20:31');

INSERT INTO `admin_user` (`id`, `username`, `password`, `nickname`, `status`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2a$10$B9B2Zms4OWnYxTm7uTZhZ..7UMPBzbMl26HlTTSNMj518KOb6CWv.', '系统管理员', 1, '2025-11-26 21:30:50', '2025-11-26 21:31:13');


INSERT INTO `asset_file` (`id`, `object_name`, `bucket`, `content_type`, `size`, `owner_id`, `created_at`, `original_name`) VALUES
(1, 'avatars/20250101/user_1_avatar.jpg', 'assets', 'image/jpeg', 102400, 1, '2025-11-26 19:53:33', 'admin_avatar.jpg'),
(2, 'avatars/20250101/user_2_avatar.jpg', 'assets', 'image/jpeg', 98304, 2, '2025-11-26 19:53:33', 'zhangsan_avatar.jpg'),
(3, 'activities/20250401/activity_1_cover.jpg', 'assets', 'image/jpeg', 256000, 1, '2025-11-26 19:53:33', 'opening_ceremony.jpg'),
(4, 'activities/20250401/activity_2_cover.jpg', 'assets', 'image/jpeg', 278000, 1, '2025-11-26 19:53:33', 'attack_defense.jpg'),
(5, 'articles/20250401/article_1_cover.jpg', 'assets', 'image/jpeg', 189000, 1, '2025-11-26 19:53:33', 'conference_news.jpg'),
(6, 'documents/20250401/conference_guide.pdf', 'assets', 'application/pdf', 5242880, 1, '2025-11-26 19:53:33', '大会指南.pdf'),
(7, 'videos/20250401/opening_highlight.mp4', 'assets', 'video/mp4', 104857600, 1, '2025-11-26 19:53:33', '开幕式精彩集锦.mp4'),
(8, 'Radial_Sku_Stock_Info_20251119064613.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 7906, 11, '2025-11-26 14:53:34', 'Radial_Sku_Stock_Info_20251119064613.xlsx'),
(9, 'test-img-size.png', 'assets', 'image/png', 119033, 11, '2025-11-26 14:54:20', 'test-img-size.png'),
(10, 'The Clinique US Store Issue Bundle Products Report_20251119.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 22400, 11, '2025-11-26 15:25:55', 'The Clinique US Store Issue Bundle Products Report_20251119.xlsx'),
(11, 'reser_failed_orders_20250916.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 30683, 11, '2025-11-26 15:28:45', 'reser_failed_orders_20250916.xlsx'),
(12, 'reser_failed_orders_20250913_new.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 30630, 11, '2025-11-26 15:31:19', 'reser_failed_orders_20250913_new.xlsx'),
(13, 'weekly_orders_by_store_20251111.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 13486, 11, '2025-11-26 15:33:37', 'weekly_orders_by_store_20251111.xlsx'),
(14, '联想截图_20250415143644.png', 'assets', 'image/png', 254079, 11, '2025-11-26 15:35:01', '联想截图_20250415143644.png'),
(15, 'OverCast_Digital-AuthenticOne_SOW_PT.pdf', 'assets', 'application/pdf', 524167, 12, '2025-11-26 16:22:07', 'OverCast_Digital-AuthenticOne_SOW_PT.pdf'),
(18, 'doctor_schedules.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 4411, 12, '2025-11-26 16:23:45', 'doctor_schedules.xlsx'),
(21, '8a0fbab4-5059-4b95-b862-f5bd5aafddf0.png', 'assets', 'image/png', 119033, 12, '2025-11-26 16:26:06', 'test-img-size.png'),
(22, '6e85a30e-d5ec-4c23-b404-fde5722b69e0.xlsx', 'assets', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 4411, 12, '2025-11-26 16:27:35', 'doctor_schedules (1).xlsx'),
(23, '6f784d33-5d42-4149-a2ea-cde4041d60dd.png', 'assets', 'image/png', 119033, 12, '2025-11-26 16:27:59', 'test-img-size.png'),
(24, '866c4eee-9917-42cf-a0af-9bf78c16d477.png', 'assets', 'image/png', 119033, 12, '2025-11-26 16:30:49', 'test-img-size.png'),
(25, '4dc3f0fe-8b38-4b2d-9407-f511b0101f90.png', 'assets', 'image/png', 119033, 12, '2025-11-26 16:32:03', 'test-img-size.png'),
(26, 'ff0cc7f3-1b0d-4498-8b2a-0a3f580e65ce.png', 'assets', 'image/png', 119033, 12, '2025-11-26 16:32:42', 'test-img-size.png'),
(27, 'a5e7e75d-58c6-453a-a147-b37b498c9bf0.pdf', 'assets', 'application/pdf', 1404166, 3, '2025-12-01 11:11:00', 'AI_Report_2790134219 (3).pdf'),
(28, '56b09e7e-dc30-4e94-a79d-82e5d74cddce.pdf', 'assets', 'application/pdf', 1388253, 3, '2025-12-01 11:11:10', 'AI_Report_2790134219 (2).pdf'),
(29, '66bfb605-64e1-434d-81cf-064d0bb75501.png', 'assets', 'image/png', 489919, 3, '2025-12-01 11:11:33', 'img.png'),
(30, '073e7faa-5227-4aac-a1ed-79111440bbab.png', 'assets', 'image/png', 489919, 3, '2025-12-01 12:18:15', 'img.png'),
(31, '631167e9-8d33-478e-a742-ef8986861518.jpg', 'assets', 'image/jpeg', 3237314, 3, '2025-12-01 12:29:15', 'MSI_MAG.jpg');

INSERT INTO `comment` (`id`, `user_id`, `target_type`, `target_id`, `content`, `created_at`) VALUES
(1, 2, 'activity', 1, '期待西湖论剑开幕式,这是网络安全行业的年度盛会!', '2025-11-26 19:53:33'),
(2, 3, 'activity', 1, '已经报名参加,希望能学到很多前沿知识。', '2025-11-26 19:53:33'),
(3, 4, 'activity', 1, '我也很期待,去年的大会非常精彩!', '2025-11-26 19:53:33'),
(4, 5, 'activity', 2, '攻防演练最吸引人,可以看到实战技术展示。', '2025-11-26 19:53:33'),
(5, 6, 'activity', 4, 'AI+安全是未来趋势,这个论坛值得关注。', '2025-11-26 19:53:33'),
(6, 7, 'article', 1, '西湖论剑一年比一年办得好,为组委会点赞!', '2025-11-26 19:53:33'),
(7, 8, 'article', 2, '网络安全市场确实在高速增长,就业前景不错。', '2025-11-26 19:53:33'),
(8, 9, 'article', 3, 'AI在安全领域大有可为,但也要警惕AI被滥用。', '2025-11-26 19:53:33'),
(9, 3, 'article', 5, '零信任架构是大势所趋,企业应该尽早布局。', '2025-11-26 19:53:33'),
(10, 4, 'article', 6, '容器安全确实很重要,我们公司最近也在加强这方面。', '2025-11-26 19:53:33'),
(11, 5, 'activity', 3, '数据安全合规越来越受重视了。', '2025-11-26 19:53:33'),
(12, 6, 'activity', 8, '网络安全人才缺口大,是个好的职业选择。', '2025-11-26 19:53:33'),
(13, 7, 'article', 8, '作为从业者,深感人才培养的重要性。', '2025-11-26 19:53:33'),
(14, 8, 'article', 9, '开源软件安全不容忽视,要建立完善的管理机制。', '2025-11-26 19:53:33'),
(15, 9, 'article', 10, '见证了西湖论剑的成长,希望越办越好!', '2025-11-26 19:53:33'),
(16, 11, 'article', 9, '\n开源伟大~', '2025-11-26 12:09:45'),
(17, 11, 'activity', 1, '伟大~', '2025-11-26 12:09:56'),
(18, 11, 'activity', 3, '\n数据安全合规越来越受重视了ao。', '2025-11-26 12:28:46'),
(19, 12, 'activity', 2, '伟大~', '2025-11-26 16:21:05'),
(20, 12, 'article', 10, '攻防演练最吸引人,可以看到实战技术展示。', '2025-11-26 16:21:17');

INSERT INTO `content_article` (`id`, `title`, `summary`, `body`, `cover_url`, `category`, `author`, `views`, `status`, `published_at`, `created_at`, `updated_at`, `live_url`, `replay_url`) VALUES
(1, '2025西湖论剑·数字安全大会即将盛大开幕', '作为国内网络安全领域的年度盛会,西湖论剑将于5月15日在杭州盛大开幕。', '<h2>大会概况</h2><p>2025西湖论剑·数字安全大会将于5月15-19日在浙江省杭州市西湖国际会议中心举行。本届大会以\"安全筑基,数智未来\"为主题,汇聚全球网络安全领域的顶尖专家、企业代表和政府官员。</p><h2>亮点活动</h2><p>大会将举办主论坛、10余场专题分论坛、网络安全创新大赛、展商展览等丰富活动。</p><h2>参会指南</h2><p>参会者可通过官网在线报名,现场将提供同声传译、会议资料下载等服务。</p>', 'https://picsum.photos/seed/article1/800/450', '通知', '大会组委会', 1245, 1, '2025-04-01 10:00:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025');
INSERT INTO `content_article` (`id`, `title`, `summary`, `body`, `cover_url`, `category`, `author`, `views`, `status`, `published_at`, `created_at`, `updated_at`, `live_url`, `replay_url`) VALUES
(2, '网络安全行业2024年度报告发布', '报告显示,全球网络安全市场规模持续增长,预计2025年将突破2000亿美元。', '<h2>市场规模</h2><p>根据最新报告,2024年全球网络安全市场规模达到1850亿美元,同比增长12.3%。</p><h2>主要趋势</h2><ul><li>AI驱动的安全解决方案快速发展</li><li>零信任架构成为主流</li><li>云安全需求持续增长</li><li>供应链安全受到重视</li></ul><h2>中国市场</h2><p>中国网络安全市场增速领先全球,预计未来三年复合增长率将达到20%以上。</p>', 'https://picsum.photos/seed/article2/800/450', '行业', '安全研究院', 892, 1, '2025-04-05 14:30:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025');
INSERT INTO `content_article` (`id`, `title`, `summary`, `body`, `cover_url`, `category`, `author`, `views`, `status`, `published_at`, `created_at`, `updated_at`, `live_url`, `replay_url`) VALUES
(3, 'AI技术在网络安全领域的应用与挑战', '人工智能正在深刻改变网络安全防御和攻击的方式,但同时也带来新的安全挑战。', '<h2>AI赋能安全防御</h2><p>机器学习算法可以实时分析海量安全数据,快速识别异常行为和潜在威胁。</p><h2>AI威胁检测</h2><p>基于深度学习的威胁检测系统能够发现传统方法难以识别的高级持续性威胁(APT)。</p><h2>面临的挑战</h2><p>AI技术本身也可能被攻击者利用,产生对抗样本攻击、模型投毒等新型安全问题。</p>', 'https://picsum.photos/seed/article3/800/450', '技术', '王研究员', 756, 1, '2025-04-10 09:15:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025');
INSERT INTO `content_article` (`id`, `title`, `summary`, `body`, `cover_url`, `category`, `author`, `views`, `status`, `published_at`, `created_at`, `updated_at`, `live_url`, `replay_url`) VALUES
(4, '数据安全法实施两周年:企业合规实践经验分享', '自《数据安全法》实施以来,企业在数据安全治理方面积累了丰富经验。', '<h2>法规要求</h2><p>《数据安全法》要求企业建立数据分类分级保护制度,开展数据安全风险评估。</p><h2>实践经验</h2><p>领先企业通过建立数据安全管理组织、制定安全策略、实施技术防护措施等方式,有效提升了数据安全水平。</p><h2>未来展望</h2><p>随着配套法规的完善,企业数据安全合规将更加规范化、体系化。</p>', 'https://picsum.photos/seed/article4/800/450', '安全', '李律师', 634, 1, '2025-04-12 16:20:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(5, '零信任架构:企业安全建设的新范式', '零信任安全模型正在成为企业数字化转型中的核心安全架构。', '<h2>零信任理念</h2><p>零信任模型基于\"永不信任,始终验证\"的原则,要求对所有访问请求进行身份验证和授权。</p><h2>关键技术</h2><ul><li>身份与访问管理(IAM)</li><li>微分段</li><li>多因素认证(MFA)</li><li>持续验证</li></ul><h2>实施路径</h2><p>企业应分阶段推进零信任架构建设,优先保护核心资产和关键业务系统。</p>', 'https://picsum.photos/seed/article5/800/450', '技术', '赵架构师', 823, 1, '2025-04-15 11:00:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(6, '云原生安全:保护容器化应用的最佳实践', '随着容器技术的普及,云原生安全成为企业关注的重点。', '<h2>容器安全挑战</h2><p>容器化应用面临镜像漏洞、运行时威胁、配置错误等安全风险。</p><h2>安全防护措施</h2><p>包括镜像扫描、运行时保护、网络隔离、安全配置管理等多层防护手段。</p><h2>DevSecOps实践</h2><p>将安全融入DevOps流程,实现安全左移,在开发阶段就识别和修复安全问题。</p>', 'https://picsum.photos/seed/article6/800/450', '技术', '周工程师', 567, 1, '2025-04-18 10:30:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(7, '工业互联网安全态势分析与应对策略', '工业互联网的快速发展带来了新的安全挑战,需要建立完善的安全保障体系。', '<h2>安全态势</h2><p>工业控制系统暴露面增大,针对工业设施的网络攻击事件频发。</p><h2>主要威胁</h2><ul><li>勒索软件攻击</li><li>供应链攻击</li><li>APT攻击</li><li>内部威胁</li></ul><h2>防护建议</h2><p>加强网络隔离、实施安全监测、定期安全评估、开展应急演练。</p>', 'https://picsum.photos/seed/article7/800/450', '安全', '孙专家', 489, 1, '2025-04-20 14:45:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(8, '网络安全人才培养新模式探索', '面对日益严峻的网络安全形势,创新人才培养模式成为行业共识。', '<h2>人才缺口</h2><p>我国网络安全人才缺口超过100万,高端人才尤其紧缺。</p><h2>培养模式</h2><p>校企合作、实战化培训、认证体系建设等多种方式并举。</p><h2>职业发展</h2><p>网络安全岗位薪资水平持续提升,职业发展路径日趋多元化。</p>', 'https://picsum.photos/seed/article8/800/450', '行业', '林教授', 712, 1, '2025-04-22 09:00:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(9, '开源软件供应链安全管理实践', '开源软件在带来便利的同时,也引入了供应链安全风险。', '<h2>风险分析</h2><p>开源组件漏洞、恶意代码注入、许可证合规等问题日益突出。</p><h2>管理措施</h2><p>建立软件物料清单(SBOM)、漏洞扫描、依赖分析、安全审计。</p><h2>工具推荐</h2><p>介绍主流的开源软件成分分析(SCA)工具及使用方法。</p>', 'https://picsum.photos/seed/article9/800/450', '技术', '何安全', 598, 1, '2025-04-25 15:30:00', '2025-11-26 19:53:32', '2025-12-01 19:45:07', 'https://live.example.com/conference-2025', 'https://replay.example.com/conference-2025'),
(10, '西湖论剑精彩回顾:历届大会亮点集锦', '回顾西湖论剑历届大会的精彩瞬间,见证中国网络安全事业的发展历程。', '<h2>发展历程</h2><p>自2006年首届举办以来,西湖论剑已成功举办18届,成为业界标志性盛会。</p><h2>经典瞬间</h2><p>历届大会涌现了众多经典议题、精彩演讲和创新成果。</p><h2>未来展望</h2><p>西湖论剑将继续引领行业发展,为网络强国建设贡献力量。</p>', 'https://picsum.photos/seed/article10/800/450', '通知', '编辑部', 1156, 1, '2025-04-28 10:00:00', '2025-11-26 19:53:32', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(12, 'asd', 'ads', 'asd', '', 'UGC', NULL, 1156, 1, NULL, '2025-11-26 22:25:19', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025'),
(13, 'asd', 'asd', 'asdasd', '', 'UGC', NULL, 0, 0, NULL, '2025-11-27 00:22:57', '2025-12-01 19:57:27', 'https://live.example.com/conference-2025', 'https://live.example.com/conference-2025');

INSERT INTO `exhibitor` (`id`, `name`, `logo_url`, `introduction`, `video_url`, `website`) VALUES
(1, '阿里云安全', 'https://img0.baidu.com/it/u=4098120469,3194021525&fm=253&fmt=auto&app=138&f=JPEG?w=420&h=280', '阿里云安全依托阿里巴巴集团20年安全技术积累,为企业提供全方位的云安全解决方案。产品涵盖DDoS防护、Web应用防火墙、安全中心、数据安全等领域,为百万级企业客户提供云上安全防护。', 'https://www.bilibili.com/video/BV1V6HczhECn/?spm_id_from=333.337.search-card.all.click', 'https://www.aliyun.com/product/security'),
(2, '腾讯安全', 'https://img0.baidu.com/it/u=3505072417,867264446&fm=253&fmt=auto&app=120&f=JPEG?w=1600&h=800', '腾讯安全致力于构建安全、可信的数字世界,业务覆盖云安全、终端安全、业务安全等领域。拥有20年互联网安全攻防经验,守护腾讯10亿级用户,为政企客户提供专业的安全服务。核心产品包括御点终端安全、御界高级威胁检测、御见威胁情报中心等。', 'https://www.bilibili.com/video/BV1jh41197SB/?spm_id_from=333.337.search-card.all.click', 'https://s.tencent.com'),
(3, '360数字安全集团', 'https://img0.baidu.com/it/u=970183104,1846745994&fm=253&fmt=auto?w=1051&h=800', '360数字安全集团是中国领先的数字安全公司,专注于网络安全、数据安全和云安全。拥有18年安全大数据积累,每天处理2000亿安全样本,在网络安全、数据安全、终端安全等领域处于领先地位。提供政企安全、城市安全、工业互联网安全等整体解决方案。', 'https://www.bilibili.com/video/BV1zS4y1p7PN/?spm_id_from=333.337.search-card.all.click', 'https://www.360.cn'),
(4, '深信服科技', 'https://img2.baidu.com/it/u=3788203300,2265208591&fm=253&fmt=auto&app=120&f=JPEG?w=223&h=99', '深信服致力于让IT更简单、安全、有价值,提供企业级安全、云计算和IT基础架构解决方案。产品包括下一代防火墙(NGAF)、终端检测响应(EDR)、云计算、广域网优化等,服务全球60000+客户,在企业安全、云安全领域具有领先优势。', 'https://www.bilibili.com/video/BV1R4nozXEXV/?spm_id_from=333.337.search-card.all.click', 'https://www.sangfor.com.cn'),
(5, '启明星辰', 'https://img0.baidu.com/it/u=143793011,3807273948&fm=253&fmt=auto&app=138&f=PNG?w=750&h=500', '启明星辰是中国网络安全行业的领军企业,提供全面的网络安全产品和服务。业务涵盖安全产品、安全服务、安全运营等领域,拥有25年安全攻防经验,为政府、金融、运营商等行业提供专业安全保障。核心产品包括防火墙、入侵检测、安全管理平台、数据安全等。', 'https://www.bilibili.com/video/BV12W4y1A7PK/?spm_id_from=333.337.search-card.all.click', 'https://www.venustech.com.cn'),
(6, '奇安信集团', 'https://img0.baidu.com/it/u=1662512579,4097580639&fm=253&fmt=auto&app=138&f=JPEG?w=667&h=500', '奇安信是中国领先的网络安全公司,为政企客户提供新一代企业级网络安全产品和服务。拥有40+款安全产品,覆盖端点安全、网络安全、安全管理、安全服务等领域,服务政府部门、央企等高端客户。在政企安全、实战攻防、威胁情报等领域处于行业领先地位。', 'https://www.bilibili.com/video/BV1Nv4y1K7mp/?spm_id_from=333.337.search-card.all.click', 'https://www.qianxin.com'),
(7, '绿盟科技', 'https://img1.baidu.com/it/u=2403732451,114229907&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=500', '绿盟科技专注于网络安全领域,提供安全产品、安全服务和安全解决方案。产品包括DDoS防护、WAF、IPS/IDS、漏洞扫描、威胁情报等,在云安全、网络空间安全、数据安全等领域具有丰富经验。连续多年入选Gartner、IDC等国际权威机构报告。', 'https://www.bilibili.com/video/BV1xq421A7ph/?spm_id_from=333.337.search-card.all.click', 'https://www.nsfocus.com.cn'),
(8, '天融信', 'https://img0.baidu.com/it/u=3687063117,329668396&fm=253&fmt=auto&app=138&f=PNG?w=475&h=316', '天融信是中国网络安全行业的开创者,在防火墙等领域具有领先地位。业务涵盖边界安全、终端安全、应用安全、数据安全等领域,为金融、政府、运营商、能源等行业提供安全保障。拥有30年安全技术积累,是国内最早的网络安全厂商之一。', 'https://www.bilibili.com/video/BV1Qz411q7Ma/?spm_id_from=333.337.search-card.all.click', 'https://www.topsec.com.cn'),
(9, '安恒信息', 'https://img2.baidu.com/it/u=2391491830,3830309120&fm=253&fmt=auto&app=138&f=JPEG?w=750&h=500', '安恒信息专注于数据安全、应用安全和云安全,为数字经济保驾护航。产品包括数据库安全、Web应用防火墙、漏洞扫描、渗透测试等,在数据安全领域处于领先地位,服务金融、政务等行业。连续多年承担国家重大活动网络安全保障任务。', 'https://www.bilibili.com/video/BV1ng41157tb/?spm_id_from=333.337.search-card.all.click', 'https://www.dbappsecurity.com.cn'),
(10, '亚信安全', 'https://img2.baidu.com/it/u=3688808501,2882889280&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500', '亚信安全专注于云安全、终端安全和威胁情报,守护5G时代的网络安全。产品包括综合威胁管理(XDR)、终端安全管理(ESM)、云工作负载保护等,服务运营商、金融、能源等关键基础设施行业。在5G安全、云原生安全等新兴领域具有技术优势。', 'https://www.bilibili.com/video/BV1qG41187bg/?spm_id_from=333.337.search-card.all.click', 'https://www.asiainfo-sec.com');

INSERT INTO `like_record` (`id`, `user_id`, `target_type`, `target_id`, `created_at`) VALUES
(1, 2, 'activity', 1, '2025-11-26 19:53:33');
INSERT INTO `like_record` (`id`, `user_id`, `target_type`, `target_id`, `created_at`) VALUES
(2, 3, 'activity', 1, '2025-11-26 19:53:33');
INSERT INTO `like_record` (`id`, `user_id`, `target_type`, `target_id`, `created_at`) VALUES
(3, 4, 'activity', 1, '2025-11-26 19:53:33');
INSERT INTO `like_record` (`id`, `user_id`, `target_type`, `target_id`, `created_at`) VALUES
(4, 5, 'activity', 1, '2025-11-26 19:53:33'),
(5, 6, 'activity', 1, '2025-11-26 19:53:33'),
(6, 2, 'activity', 2, '2025-11-26 19:53:33'),
(7, 3, 'activity', 2, '2025-11-26 19:53:33'),
(8, 4, 'activity', 4, '2025-11-26 19:53:33'),
(9, 5, 'activity', 4, '2025-11-26 19:53:33'),
(10, 6, 'activity', 4, '2025-11-26 19:53:33'),
(11, 7, 'activity', 10, '2025-11-26 19:53:33'),
(12, 8, 'activity', 10, '2025-11-26 19:53:33'),
(13, 9, 'activity', 10, '2025-11-26 19:53:33'),
(14, 2, 'article', 1, '2025-11-26 19:53:33'),
(15, 3, 'article', 1, '2025-11-26 19:53:33'),
(16, 4, 'article', 2, '2025-11-26 19:53:33'),
(17, 5, 'article', 3, '2025-11-26 19:53:33'),
(18, 6, 'article', 3, '2025-11-26 19:53:33'),
(19, 7, 'article', 5, '2025-11-26 19:53:33'),
(20, 8, 'article', 6, '2025-11-26 19:53:33'),
(21, 9, 'article', 8, '2025-11-26 19:53:33'),
(22, 3, 'article', 10, '2025-11-26 19:53:33'),
(23, 4, 'article', 10, '2025-11-26 19:53:33'),
(24, 5, 'article', 10, '2025-11-26 19:53:33'),
(25, 11, 'activity', 10, '2025-11-26 12:12:57'),
(26, 11, 'article', 10, '2025-11-26 12:15:28'),
(27, 11, 'article', 9, '2025-11-26 12:16:57'),
(28, 11, 'activity', 6, '2025-11-26 12:17:01'),
(29, 11, 'activity', 5, '2025-11-26 12:17:05'),
(30, 11, 'activity', 1, '2025-11-26 12:17:09'),
(31, 11, 'activity', 9, '2025-11-26 12:17:16'),
(32, 11, 'activity', 3, '2025-11-26 12:23:10'),
(33, 11, 'comment', 4, '2025-11-26 14:14:32'),
(34, 11, 'comment', 11, '2025-11-26 14:21:12'),
(35, 12, 'comment', 19, '2025-11-26 16:21:07'),
(36, 12, 'activity', 2, '2025-11-26 16:21:08'),
(37, 12, 'comment', 20, '2025-11-26 16:21:18'),
(38, 12, 'article', 10, '2025-11-26 16:21:19'),
(39, 3, 'comment', 1, '2025-12-01 11:14:28'),
(40, 3, 'comment', 2, '2025-12-01 11:14:29'),
(41, 3, 'comment', 17, '2025-12-01 11:14:30'),
(42, 3, 'article', 9, '2025-12-01 11:14:35'),
(43, 3, 'article', 8, '2025-12-01 11:27:28'),
(44, 3, 'article', 2, '2025-12-01 11:27:37'),
(45, 3, 'article', 7, '2025-12-01 11:29:43'),
(46, 3, 'article', 6, '2025-12-01 11:29:46'),
(47, 3, 'article', 3, '2025-12-01 11:29:48'),
(48, 3, 'comment', 8, '2025-12-01 12:01:30');

INSERT INTO `material` (`id`, `title`, `speaker`, `object_name`, `content_type`, `size`, `created_at`) VALUES
(3, 'ppt资料', 'lisa', '6e85a30e-d5ec-4c23-b404-fde5722b69e0.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 4411, '2025-11-26 16:27:39');


INSERT INTO `media_asset` (`id`, `title`, `summary`, `type`, `object_name`, `cover_url`, `article_id`, `created_at`) VALUES
(3, 'image', 'ss', 'image', '6f784d33-5d42-4149-a2ea-cde4041d60dd.png', '', NULL, '2025-11-26 16:28:00');
INSERT INTO `media_asset` (`id`, `title`, `summary`, `type`, `object_name`, `cover_url`, `article_id`, `created_at`) VALUES
(4, 'sss', '', 'image', '073e7faa-5227-4aac-a1ed-79111440bbab.png', '', NULL, '2025-12-01 12:18:15');
INSERT INTO `media_asset` (`id`, `title`, `summary`, `type`, `object_name`, `cover_url`, `article_id`, `created_at`) VALUES
(5, 'wqq', '', 'image', '631167e9-8d33-478e-a742-ef8986861518.jpg', '', 9, '2025-12-01 12:29:15');



INSERT INTO `share_record` (`id`, `user_id`, `target_type`, `target_id`, `channel`, `created_at`) VALUES
(1, 2, 'activity', 1, 'wechat', '2025-11-26 19:53:33'),
(2, 3, 'activity', 1, 'weibo', '2025-11-26 19:53:33'),
(3, 4, 'activity', 2, 'wechat', '2025-11-26 19:53:33'),
(4, 5, 'activity', 4, 'qq', '2025-11-26 19:53:33'),
(5, 6, 'article', 1, 'wechat', '2025-11-26 19:53:33'),
(6, 7, 'article', 3, 'wechat', '2025-11-26 19:53:33'),
(7, 8, 'article', 5, 'weibo', '2025-11-26 19:53:33'),
(8, 9, 'activity', 10, 'wechat', '2025-11-26 19:53:33'),
(9, 11, 'activity', 2, 'link', '2025-11-26 14:07:16'),
(10, 11, 'activity', 3, 'link', '2025-11-26 14:21:13'),
(11, 11, 'activity', 3, 'link', '2025-11-26 14:21:19'),
(12, 11, 'activity', 3, 'link', '2025-11-26 14:21:31'),
(13, 11, 'activity', 3, 'link', '2025-11-26 14:22:04'),
(14, 11, 'activity', 3, 'link', '2025-11-26 14:23:15'),
(15, 11, 'activity', 3, 'link', '2025-11-26 14:23:23'),
(16, 11, 'article', 3, 'link', '2025-11-26 14:27:54'),
(17, 12, 'article', 10, 'link', '2025-11-26 16:21:20');

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `email`, `phone`, `gender`, `status`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 'admin@sword-shield.com', '13800138000', '1', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `email`, `phone`, `gender`, `status`, `created_at`, `updated_at`) VALUES
(2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', 'zhangsan@example.com', '13800138001', '1', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `email`, `phone`, `gender`, `status`, `created_at`, `updated_at`) VALUES
(3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi', 'lisi@example.com', '13800138002', '2', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `email`, `phone`, `gender`, `status`, `created_at`, `updated_at`) VALUES
(4, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu', 'wangwu@example.com', '13800138003', '1', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(5, 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhaoliu', 'zhaoliu@example.com', '13800138004', '2', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(6, 'sunqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙七', 'https://api.dicebear.com/7.x/avataaars/svg?seed=sunqi', 'sunqi@example.com', '13800138005', '1', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(7, 'zhouba', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周八', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhouba', 'zhouba@example.com', '13800138006', '2', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(8, 'wujiu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴九', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wujiu', 'wujiu@example.com', '13800138007', '1', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(9, 'zhengshi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '郑十', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhengshi', 'zhengshi@example.com', '13800138008', '2', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(10, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 'https://api.dicebear.com/7.x/avataaars/svg?seed=testuser', 'test@example.com', '13800138009', '0', 1, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(11, '123@qq.com', '$2a$10$B9B2Zms4OWnYxTm7uTZhZ..7UMPBzbMl26HlTTSNMj518KOb6CWv.', '爱你yo~', '/static/default-avatar.png', '111@qq.com', '18598399898', '男', 1, '2025-11-26 11:55:13', '2025-11-26 11:55:13'),
(12, 'zhansan', '$2a$10$.XYIHH.6XywRgfQGvtSzcuMGNurlHUQ5L3da8bM5XtDSG4eXZlQLy', '爱你', 'http://172.25.255.76:9000/assets/ff0cc7f3-1b0d-4498-8b2a-0a3f580e65ce.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20251126%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20251126T163242Z&X-Amz-Expires=300&X-Amz-SignedHeaders=host&X-Amz-Signature=8e08f6747a2818cdafcb6b21444423feee9a9e74ec1c0204af35176686c86a20', '222@qq.com', '18323434556', '男', 1, '2025-11-26 16:19:24', '2025-11-26 16:19:24');

INSERT INTO `user_checkin` (`id`, `user_id`, `checkin_date`) VALUES
(1, 1, '2025-11-26'),
(2, 2, '2025-11-26'),
(3, 3, '2025-11-25'),
(4, 4, '2025-11-24'),
(5, 5, '2025-11-26'),
(6, 6, '2025-11-25'),
(7, 7, '2025-11-26'),
(8, 8, '2025-11-23'),
(9, 9, '2025-11-25'),
(10, 10, '2025-11-26'),
(11, 11, '2025-11-26'),
(12, 11, '2025-11-27'),
(13, 12, '2025-11-27'),
(14, 3, '2025-12-01');

INSERT INTO `user_points` (`id`, `user_id`, `total`, `available`, `used`, `created_at`, `updated_at`) VALUES
(1, 1, 500, 500, 0, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `user_points` (`id`, `user_id`, `total`, `available`, `used`, `created_at`, `updated_at`) VALUES
(2, 2, 320, 220, 100, '2025-11-26 19:53:32', '2025-11-26 19:53:32');
INSERT INTO `user_points` (`id`, `user_id`, `total`, `available`, `used`, `created_at`, `updated_at`) VALUES
(3, 3, 460, 460, 0, '2025-11-26 19:53:32', '2025-12-01 11:31:53');
INSERT INTO `user_points` (`id`, `user_id`, `total`, `available`, `used`, `created_at`, `updated_at`) VALUES
(4, 4, 180, 80, 100, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(5, 5, 600, 400, 200, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(6, 6, 150, 150, 0, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(7, 7, 280, 280, 0, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(8, 8, 420, 320, 100, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(9, 9, 350, 250, 100, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(10, 10, 100, 100, 0, '2025-11-26 19:53:32', '2025-11-26 19:53:32'),
(11, 11, 20, 20, 0, '2025-11-26 11:58:28', '2025-11-26 16:13:29'),
(12, 12, 10, 10, 0, '2025-11-26 16:22:59', '2025-11-26 16:23:00');


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
