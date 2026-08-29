-- =====================================================================
-- 10连抽 相关库表变更脚本
-- 说明：
--   1) user_raffle_order 为分库分表（big_market_01 / big_market_02 各含 _000 / _001）
--      故 raffle_type 列需在两个分库的两张分表上分别执行。
--   2) rule_guaranteed 为累计次数保底规则（模型B），规则值格式："100:120 300:121"
--      表示累计第100抽必得120号奖、第300抽必得121号奖。基于总账户累计次数判断。
-- =====================================================================

-- --------------------------------------------------------------------
-- 一、订单表新增抽奖类型字段 raffle_type（single-单抽 / ten-十连抽）
-- --------------------------------------------------------------------
USE `big_market_01`;
ALTER TABLE `user_raffle_order_000`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_001`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_002`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_003`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;

USE `big_market_02`;
ALTER TABLE `user_raffle_order_000`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_001`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_002`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;
ALTER TABLE `user_raffle_order_003`
    ADD COLUMN `raffle_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '抽奖类型；single-单抽、ten-十连抽' AFTER `order_state`;

-- --------------------------------------------------------------------
-- 二、配置累计保底规则 rule_guaranteed（示例：策略 100006）
--    - rule_type=1 策略规则（无需 award_id）
--    - rule_value：累计第10抽必得106号奖、第30抽必得107号奖（106/107 需在 strategy_award 中已配置且有库存）
-- --------------------------------------------------------------------
USE `big_market`;

INSERT INTO `strategy_rule` (`strategy_id`, `award_id`, `rule_type`, `rule_model`, `rule_value`, `rule_desc`, `create_time`, `update_time`)
VALUES (100006, NULL, 1, 'rule_guaranteed', '10:106 30:107', '累计次数保底；第10抽必得106、第30抽必得107', now(), now());

-- 将 rule_guaranteed 装入责任链（黑名单 -> 保底 -> 权重；默认链由工厂自动追加）
UPDATE `strategy` SET `rule_models` = 'rule_blacklist,rule_guaranteed,rule_weight', `update_time` = now()
WHERE `strategy_id` = 100006;

-- 注意：更新 strategy / strategy_rule 后，需清理 Redis 中对应策略缓存（big_market_strategy_key_、责任链缓存等）后重新装配。

-- --------------------------------------------------------------------
-- 三、扩宽中奖记录表 order_id 列宽（B1方案：10连抽子orderId = 批次号_序号，如 923847561234_01，长度15）
--    user_award_record 为分库分表（big_market_01 / big_market_02 各含 _000 ~ _003）
--    原 order_id 为 varchar(12)，需扩至 varchar(20) 以容纳子号；uq_order_id 唯一键保持不变（子号天然唯一，保证发奖幂等）
-- --------------------------------------------------------------------
USE `big_market_01`;
ALTER TABLE `user_award_record_000` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_001` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_002` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_003` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';

USE `big_market_02`;
ALTER TABLE `user_award_record_000` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_001` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_002` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';
ALTER TABLE `user_award_record_003` MODIFY COLUMN `order_id` VARCHAR(20) NOT NULL COMMENT '抽奖订单ID【作为幂等使用；10连抽为 批次号_序号】';

-- --------------------------------------------------------------------
-- 四、账户表新增"真实已抽次数" used_count（used_count 方案）
--    背景：额度在"下单"时扣、抽奖在"落库"时兑现，中间失败会产生"已扣额度但未兑现"的悬空量，
--          若用 额度口径(count - surplus) 算抽奖次数会被污染（复用/改单抽次数虚高）。
--    解法：额度字段(surplus)只管"能不能抽"（前置闸门不变）；used_count 只在落库成功事务里 +N，只管"抽到第几次"。
--    - raffle_activity_account.used_count      累计已抽（给保底 rule_guaranteed），永不重置
--    - raffle_activity_account_day.used_count  当日已抽（给 rule_lock），靠 day 字段天然跨天隔离，不做重置
--    两表均为分库分表（big_market_01 / big_market_02 各含 _000 ~ _003）
-- --------------------------------------------------------------------
USE `big_market_01`;
ALTER TABLE `raffle_activity_account` ADD COLUMN `used_count` INT NOT NULL DEFAULT 0 COMMENT '真实已抽次数（累计，落库兑现时+N）';

ALTER TABLE `raffle_activity_account_day` ADD COLUMN `used_count` INT NOT NULL DEFAULT 0 COMMENT '真实已抽次数（当日，落库兑现时+N）';


USE `big_market_02`;
ALTER TABLE `raffle_activity_account` ADD COLUMN `used_count` INT NOT NULL DEFAULT 0 COMMENT '真实已抽次数（累计，落库兑现时+N）';

ALTER TABLE `raffle_activity_account_day` ADD COLUMN `used_count` INT NOT NULL DEFAULT 0 COMMENT '真实已抽次数（当日，落库兑现时+N）';

