package cn.gzy.domain.strategy.service.rule.chain;


import cn.gzy.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖策略规则责任链接口
 * @create 2024-01-20 09:40
 */
public interface ILogicChain extends ILogicChaimArmory,Cloneable{

    /**
     * 责任链接口
     *
     * @param strategyId            策略ID
     * @param userId                用户ID
     * @param totalUserRaffleCount  累计抽奖次数【10连抽保底场景由编排层下发，单抽为 null 时节点回退查库】
     * @param endDateTime           活动结束时间【保底节点扣减库存加锁使用】
     * @return 奖品ID
     */
    DefaultChainFactory.StrategyAwardVO logic(Long strategyId, String userId, Long totalUserRaffleCount, Date endDateTime);


}
