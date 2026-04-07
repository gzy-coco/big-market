package cn.gzy.domain.strategy.service.rule.tree.factory;


import cn.gzy.domain.strategy.model.entity.StrategyAwardEntity;
import cn.gzy.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.gzy.domain.strategy.model.valobj.RuleTreeVO;
import cn.gzy.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.gzy.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 规则树工厂
 * @create 2024-01-27 11:28
 */

@Service
public class DefaultTreeFactory {

    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

    public DefaultTreeFactory(Map<String, ILogicTreeNode> logicTreeNodeGroup) {
        this.logicTreeNodeGroup = logicTreeNodeGroup;
    }


    public DecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO){
        return new DecisionTreeEngine(logicTreeNodeGroup,ruleTreeVO);
    }


    /**
     * 决策树个动作实习
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TreeActionEntity {
        private RuleLogicCheckTypeVO ruleLogicCheckType;
        private StrategyAwardVO strategyAwardData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StrategyAwardVO {
        /** 抽奖奖品ID - 内部流转使用 */
        private Integer awardId;
        /** 抽奖奖品规则 */
        private String awardRuleValue;
    }


}
