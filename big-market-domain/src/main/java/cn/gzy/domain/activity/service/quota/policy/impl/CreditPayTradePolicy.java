package cn.gzy.domain.activity.service.quota.policy.impl;


import cn.gzy.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.gzy.domain.activity.model.valobj.OrderStateVO;
import cn.gzy.domain.activity.repository.IActivityRepository;
import cn.gzy.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;

    public CreditPayTradePolicy(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }
    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
        activityRepository.doSaveCreditPayOrder(createQuotaOrderAggregate);

    }
}
