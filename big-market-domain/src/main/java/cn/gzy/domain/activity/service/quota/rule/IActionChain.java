package cn.gzy.domain.activity.service.quota.rule;

import cn.gzy.domain.activity.model.entity.ActivityCountEntity;
import cn.gzy.domain.activity.model.entity.ActivityEntity;
import cn.gzy.domain.activity.model.entity.ActivitySkuEntity;

public interface IActionChain extends IActionChainArmory{

    boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
}
