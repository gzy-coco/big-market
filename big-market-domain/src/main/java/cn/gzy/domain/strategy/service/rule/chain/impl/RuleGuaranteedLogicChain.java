package cn.gzy.domain.strategy.service.rule.chain.impl;

import cn.gzy.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.gzy.domain.strategy.repository.IStrategyRepository;
import cn.gzy.domain.strategy.service.armory.IStrategyDispatch;
import cn.gzy.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.gzy.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.gzy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author gzy
 * @description 累计次数保底责任链（模型B：累计抽到第N抽必得指定奖品）
 * 规则值格式：100:120 300:121 600:122（累计第100抽必得120号奖、第300抽必得121号...）
 * 说明：
 * 1. 累计次数 totalUserRaffleCount 由编排层通过 Redis 原子自增下发（10连抽），单抽为 null 时回退查库。
 * 2. 命中保底线后本节点直接接管，责任链接管会跳过决策树，因此库存扣减在本节点内完成（复用 rule_stock 逻辑）。
 * 3. 保底奖品缺货则降级为兜底积分（复用 rule_luck_award：101, "1,100"），不视为异常。
 * @create 2026-08-24
 */
@Slf4j
@Component("rule_guaranteed")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RuleGuaranteedLogicChain extends AbstractLogicChain {

    /** 保底奖品缺货时的兜底奖品ID（与 rule_luck_award 保持一致） */
    private static final Integer LUCK_AWARD_ID = 101;
    private static final String LUCK_AWARD_RULE_VALUE = "1,100";

    @Resource
    private IStrategyRepository repository;

    @Resource
    private IStrategyDispatch strategyDispatch;

    @Override
    public DefaultChainFactory.StrategyAwardVO logic(Long strategyId, String userId, Long totalUserRaffleCount, Date endDateTime) {
        log.info("抽奖责任链-保底开始 userId:{} strategyId:{} ruleModel:{}", userId, strategyId, ruleModel());

        // 1. 累计次数：优先用编排层下发的注入值（10连抽），为空则回退查库（单抽）
        long totalCount = null != totalUserRaffleCount
                ? totalUserRaffleCount
                : repository.queryActivityAccountTotalUseCount(userId, strategyId);

        // 2. 查询保底规则配置并判断是否命中保底线
        String ruleValue = repository.queryStrategyRuleValue(strategyId, ruleModel());
        Integer guaranteedAwardId = hit(ruleValue, totalCount);
        if (null == guaranteedAwardId) {
            // 未命中保底线，放行给下一节点
            log.info("抽奖责任链-保底放行 userId:{} strategyId:{} totalCount:{}", userId, strategyId, totalCount);
            return next().logic(strategyId, userId, totalUserRaffleCount, endDateTime);
        }

        // 3. 命中保底线 → 接管；由于接管会跳过决策树，故此处自行扣减保底奖品库存
        log.info("抽奖责任链-保底接管 userId:{} strategyId:{} totalCount:{} guaranteedAwardId:{}", userId, strategyId, totalCount, guaranteedAwardId);
        Boolean status = strategyDispatch.subtractionAwardStock(strategyId, guaranteedAwardId, endDateTime);
        if (Boolean.TRUE.equals(status)) {
            // 扣减成功：写入延迟队列，异步更新数据库库存（与 rule_stock 一致）
            repository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
                    .strategyId(strategyId)
                    .awardId(guaranteedAwardId)
                    .build());
            return DefaultChainFactory.StrategyAwardVO.builder()
                    .awardId(guaranteedAwardId)
                    .logicModel(ruleModel())
                    .build();
        }

        // 4. 保底奖品缺货 → 降级兜底积分（与 rule_luck_award 一致），非异常
        log.warn("抽奖责任链-保底奖品库存不足，降级兜底 userId:{} strategyId:{} guaranteedAwardId:{}", userId, strategyId, guaranteedAwardId);
        repository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
                .strategyId(strategyId)
                .awardId(LUCK_AWARD_ID)
                .build());
        return DefaultChainFactory.StrategyAwardVO.builder()
                .awardId(LUCK_AWARD_ID)
                .awardRuleValue(LUCK_AWARD_RULE_VALUE)
                .logicModel(ruleModel())
                .build();
    }

    /**
     * 精确命中判定：累计次数 == 某条保底线 则返回对应奖品ID，否则 null。
     * 用 == 而非 >=：每条保底线恰好被"第N抽"这一抽命中一次，避免越过后反复触发、也便于幂等。
     */
    private Integer hit(String ruleValue, long totalCount) {
        if (null == ruleValue || ruleValue.isEmpty()) return null;
        for (String part : ruleValue.split(Constants.SPACE)) {
            if (part.isEmpty()) continue;
            String[] kv = part.split(Constants.COLON);
            if (kv.length != 2) continue;
            if (Long.parseLong(kv[0].trim()) == totalCount) {
                return Integer.valueOf(kv[1].trim());
            }
        }
        return null;
    }

    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_GUARANTEED.getCode();
    }
}
