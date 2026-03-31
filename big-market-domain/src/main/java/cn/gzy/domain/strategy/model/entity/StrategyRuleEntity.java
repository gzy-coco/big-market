package cn.gzy.domain.strategy.model.entity;


import cn.gzy.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyRuleEntity {
    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖奖品ID【规则类型为策略，则不需要奖品ID】 */
    private Integer awardId;
    /** 抽象规则类型；1-策略规则、2-奖品规则 */
    private Integer ruleType;
    /** 抽奖规则类型【rule_random - 随机值计算、rule_lock - 抽奖几次后解锁、rule_luck_award - 幸运奖(兜底奖品)】 */
    private String ruleModel;
    /** 抽奖规则比值 */
    private String ruleValue;
    /** 抽奖规则描述 */
    private String ruleDesc;


    /**
     * 获取权重值
     * 数据案例；4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
     */
    public Map<String, List<Integer>> getRuleWeightValues(){
        if(!ruleModel.equals("rule_weight")) return null;
        String[] ruleValueGroups = ruleValue.split(Constants.SPACE);
        Map<String,List<Integer>> resultMap = new HashMap<>();
        for(String ruleValueGroup : ruleValueGroups){
            // 检查输入是否为空
            if (ruleValueGroup == null || ruleValueGroup.isEmpty()) {
                return resultMap;
            }

            String[] keyValue = ruleValueGroup.split(Constants.COLON);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("rule_weight rule_rule invalid input format" + ruleValueGroup);
            }

            String[] valueArr = keyValue[1].split(Constants.SPLIT);
            List<Integer> list = new ArrayList<>();
            for(String value : valueArr ){
                list.add(Integer.valueOf(value));
            }
            resultMap.put(ruleValueGroup,list);
        }
        return resultMap;

    }

}
