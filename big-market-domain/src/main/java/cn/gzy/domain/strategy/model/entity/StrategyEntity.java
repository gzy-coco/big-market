package cn.gzy.domain.strategy.model.entity;

import cn.gzy.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略实体
 * @create 2023-12-31 15:24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyEntity {

    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖策略描述 */
    private String strategyDesc;
    /** 抽奖规则模型 */
    private String ruleModels;

    public String[] ruleModels() {
        if (StringUtils.isBlank(ruleModels)) return null;
        return ruleModels.split(Constants.SPLIT);
    }
    public String getRuleWeight(){
        String[] models = ruleModels();
        if(models == null) return null;
        for(String model : models){
            if(model.equals("rule_weight")) return model;
        }
        return null;
    }

}
