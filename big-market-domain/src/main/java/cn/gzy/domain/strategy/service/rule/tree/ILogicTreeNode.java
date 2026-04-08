package cn.gzy.domain.strategy.service.rule.tree;

import cn.gzy.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 规则树接口
 * @create 2024-01-27 11:14
 */
public interface ILogicTreeNode {

    DefaultTreeFactory.TreeActionEntity  logic(Long strategyId, String userId, Integer awardId,String ruleValue);
}
