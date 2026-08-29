package cn.gzy.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖因子实体
 * @create 2024-01-06 09:20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleFactorEntity {

    /** 用户ID */
    private String userId;
    /** 策略ID */
    private Long strategyId;
    /** 结束时间 */
    private Date endDateTime;
    /**
     * 当日抽奖次数【10连抽场景使用】；由编排层通过 Redis 原子自增下发的"这一抽是今日第几次"。
     * 供决策树 rule_lock 节点解锁判断使用；为 null 时 rule_lock 回退查询数据库（单抽场景）。
     */
    private Integer todayUserRaffleCount;
    /**
     * 累计抽奖次数【10连抽保底场景使用】；由编排层通过 Redis 原子自增下发的"这一抽是累计第几次"。
     * 供保底责任链 rule_guaranteed 节点判断是否命中保底线使用。
     */
    private Long totalUserRaffleCount;
}
