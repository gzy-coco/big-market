package cn.gzy.domain.rebate.repository;

import cn.gzy.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.gzy.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.gzy.domain.rebate.model.valobj.DailyBehaviorRebateVO;

import java.util.List;

public interface IRebateRepository {

    List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(String behaviorType);

    void saveUserRebateRecord(String UserId, List<BehaviorRebateAggregate> behaviorRebateAggregates);

    List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo);
}
