package cn.gzy.domain.strategy.service.rule.tree.impl;


import cn.gzy.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.gzy.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.gzy.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import org.springframework.stereotype.Component;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 兜底奖励节点
 * @create 2024-01-27 11:23
 */
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(Long strategyId, String userId, Integer awardId) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardData(DefaultTreeFactory.StrategyAwardData.builder()
                        .awardId(101)
                        .awardRuleValue("1,100")
                        .build())
                .build();
    }
}
