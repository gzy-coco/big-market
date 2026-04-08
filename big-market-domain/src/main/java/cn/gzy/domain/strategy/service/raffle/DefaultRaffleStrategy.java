package cn.gzy.domain.strategy.service.raffle;

import cn.gzy.domain.strategy.model.valobj.RuleTreeVO;
import cn.gzy.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.gzy.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.gzy.domain.strategy.repository.IStrategyRepository;
import cn.gzy.domain.strategy.service.AbstractRaffleStrategy;
import cn.gzy.domain.strategy.service.armory.IStrategyDispatch;
import cn.gzy.domain.strategy.service.rule.chain.ILogicChain;

import cn.gzy.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

import cn.gzy.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.gzy.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 默认的抽奖策略实现
 * @create 2024-01-06 11:46
 */
@Service
@Slf4j
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {


    public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory) {
        super(repository, strategyDispatch, defaultChainFactory, defaultTreeFactory);
    }

    @Override
    public DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);

        return logicChain.logic(strategyId,userId);
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId) {
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModels(strategyId,awardId);
        if (null == strategyAwardRuleModelVO) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        RuleTreeVO ruleTreeVO = repository.queryRuleTreeVOByTreeId(strategyAwardRuleModelVO.getRuleModels());

        if (null == ruleTreeVO) {
            throw new RuntimeException("存在抽奖策略配置的规则模型 Key，未在库表 rule_tree、rule_tree_node、rule_tree_line 配置对应的规则树信息 " + strategyAwardRuleModelVO.getRuleModels());
        }
        DecisionTreeEngine decisionTreeEngine = defaultTreeFactory.openLogicTree(ruleTreeVO);
        return decisionTreeEngine.process(userId,strategyId,awardId);
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
        return repository.takeQueueValue();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        repository.updateStrategyAwardStock(strategyId, awardId);
    }



//    @Override
//    protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity, String... logics) {
//        //无抽奖前置过滤规则，则直接放行
//        if(logics == null || logics.length == 0) return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
//                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
//                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
//                .build();
//
//        Map<String, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilterGroup =  defaultLogicFactory.openLogicFilter();
//
//        // 黑名单规则优先过滤
//        //logics {rule_weight,rule_blacklist}
//        String ruleBackList = Arrays.stream(logics)
//                .filter(logic-> DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(logic))
//                .findFirst()
//                .orElse(null);
//        if(ruleBackList != null){
//            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterGroup.get(ruleBackList);
//            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
//            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
//            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
//            ruleMatterEntity.setRuleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode());
//            ruleMatterEntity.setAwardId(ruleMatterEntity.getAwardId());  // 奖品id没啥用
//            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity  = logicFilter.filter(ruleMatterEntity);
//            if(!ruleActionEntity.getCode().equals(RuleLogicCheckTypeVO.ALLOW.getCode())){
//                return ruleActionEntity;
//            }
//        }
//        //顺序过滤剩余规则
//        List<String> ruleList = Arrays.stream(logics)
//                .filter(logic->!DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(logic))
//                .collect(Collectors.toList());
//        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity = null;
//        for(String rule : ruleList){
//            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterGroup.get(rule);
//            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
//            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
//            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
//            ruleMatterEntity.setRuleModel(rule);
//            ruleMatterEntity.setAwardId(ruleMatterEntity.getAwardId());  // 奖品id没啥用
//            ruleActionEntity  = logicFilter.filter(ruleMatterEntity);
//            // 非放行结果则顺序过滤
//            log.info("抽奖前规则过滤 userId: {} ruleModel: {} code: {} info: {}", raffleFactorEntity.getUserId(), rule, ruleActionEntity.getCode(), ruleActionEntity.getInfo());
//            if(!ruleActionEntity.getCode().equals(RuleLogicCheckTypeVO.ALLOW.getCode())){
//                return ruleActionEntity;
//            }
//        }
//
//        return ruleActionEntity;
//    }



//    protected RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity, String... logics) {
//        if (logics == null || 0 == logics.length) return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
//                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
//                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
//                .build();
//
//        Map<String, ILogicFilter<RuleActionEntity.RaffleCenterEntity>> logicFilterGroup = defaultLogicFactory.openLogicFilter();
//
//        RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleActionEntity = null;
//        for (String ruleModel : logics) {
//            ILogicFilter<RuleActionEntity.RaffleCenterEntity> logicFilter = logicFilterGroup.get(ruleModel);
//            RuleMatterEntity ruleMatterEntity = new RuleMatterEntity();
//            ruleMatterEntity.setUserId(raffleFactorEntity.getUserId());
//            ruleMatterEntity.setAwardId(raffleFactorEntity.getAwardId());
//            ruleMatterEntity.setStrategyId(raffleFactorEntity.getStrategyId());
//            ruleMatterEntity.setRuleModel(ruleModel);
//            ruleActionEntity = logicFilter.filter(ruleMatterEntity);
//            // 非放行结果则顺序过滤
//            log.info("抽奖中规则过滤 userId: {} ruleModel: {} code: {} info: {}", raffleFactorEntity.getUserId(), ruleModel, ruleActionEntity.getCode(), ruleActionEntity.getInfo());
//            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleActionEntity.getCode())) return ruleActionEntity;
//        }
//        return ruleActionEntity;
//    }
}
