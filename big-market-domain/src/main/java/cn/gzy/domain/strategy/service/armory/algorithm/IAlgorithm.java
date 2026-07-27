package cn.gzy.domain.strategy.service.armory.algorithm;

import cn.gzy.domain.strategy.model.entity.StrategyAwardEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖算法
 * @create 2024-08-24 13:02
 */
public interface IAlgorithm {

    /**
     *  抽奖策略装配
     * @param key
     * @param strategyAwardEntities
     * @param rateRange
     */
    void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardEntities, BigDecimal rateRange);

    /**
     * 抽奖
     * @param key
     * @return
     */
    Integer dispatchAlgorithm(String key);

}
