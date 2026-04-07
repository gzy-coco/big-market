package cn.gzy.domain.strategy.repository;

import cn.gzy.domain.strategy.model.entity.StrategyAwardEntity;
import cn.gzy.domain.strategy.model.entity.StrategyEntity;
import cn.gzy.domain.strategy.model.entity.StrategyRuleEntity;
import cn.gzy.domain.strategy.model.valobj.RuleTreeVO;
import cn.gzy.domain.strategy.model.valobj.StrategyAwardRuleModelVO;

import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略服务仓储接口
 * @create 2023-12-23 09:33
 */
public interface IStrategyRepository {

    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> strategyAwardSearchRateTable);

    Integer getStrategyAwardAssemble(String key, Integer rateKey);

    int getRateRange(Long strategyId);

    int getRateRange(String key);

    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    StrategyRuleEntity queryStrategyRule(Long strategyId,String ruleWeight);

    String queryStrategyRuleValue(Long strategyId,String ruleModel,Integer awardId);

    String queryStrategyRuleValue(Long strategyId,String ruleModel);

    StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId,Integer awardId);


    /**
     * 根据规则树ID，查询树结构信息
     *
     * @param treeId 规则树ID
     * @return 树结构信息
     */
    RuleTreeVO queryRuleTreeVOByTreeId(String treeId);

}
