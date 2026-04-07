package cn.gzy.domain.strategy.service.rule.chain.impl;

import cn.gzy.domain.strategy.repository.IStrategyRepository;
import cn.gzy.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.gzy.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.gzy.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import cn.gzy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 黑名单责任链
 * @create 2024-01-20 10:23
 */
@Slf4j
@Component("rule_blacklist")
public class BackListLogicChain extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(Long strategyId, String userId) {
        log.info("抽奖责任链-黑名单开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());

        // 查询规则值配置
        // 100 : user001,user002,user003    100 为awardId
        String ruleValue = repository.queryStrategyRuleValue(strategyId,ruleModel());
        Integer awardId = Integer.valueOf(ruleValue.split(Constants.COLON)[0]);
        String[] userIds = ruleValue.split(Constants.COLON)[1].split(Constants.SPLIT);
        for(String id : userIds){
            if(id.equals(userId)){
                log.info("抽奖责任链-黑名单接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
                return DefaultChainFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .logicModel(ruleModel())
                        .build();
            }
        }
        // 过滤其他责任链
        log.info("抽奖责任链-黑名单放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        return next().logic(strategyId,userId);
    }

    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_BLACKLIST.getCode();
    }
}
