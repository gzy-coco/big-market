package cn.gzy.domain.strategy.service.rule.impl;

import cn.gzy.domain.strategy.model.entity.RuleActionEntity;
import cn.gzy.domain.strategy.model.entity.RuleMatterEntity;
import cn.gzy.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.gzy.domain.strategy.repository.IStrategyRepository;
import cn.gzy.domain.strategy.service.annotation.LogicStrategy;
import cn.gzy.domain.strategy.service.rule.ILogicFilter;
import cn.gzy.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.gzy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 【抽奖前规则】黑名单用户过滤规则
 * @create 2024-01-06 13:19
 */
@Service
@Slf4j
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_BLACKLIST)
public class RuleBackListLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    @Resource
    private IStrategyRepository repository;
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-黑名单 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        // 查询规则值配置
        // ruleValue为 awardId:userId1,userId2,userId3,.....
        // 表示奖品id为awardId 拉黑的用户id列表
        String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getRuleModel(),ruleMatterEntity.getAwardId());
        String awardId = ruleValue.split(Constants.COLON)[0];

        // 过滤其他规则
        String[] userIdArr = ruleValue.split(Constants.COLON)[1].split(Constants.SPLIT);
        for(String userId : userIdArr){
            // 在被拉黑名单中
            if(userId.equals(ruleMatterEntity.getUserId())){
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                        .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                        .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                        .data(RuleActionEntity.RaffleBeforeEntity.builder()
                                .strategyId(ruleMatterEntity.getStrategyId())
                                .awardId(Integer.parseInt(awardId))
                                .build())
                        .build();
            }
        }
        // 没有被拉黑
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }
}
