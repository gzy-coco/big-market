package cn.gzy.domain.activity.repository;

import cn.gzy.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.gzy.domain.activity.model.entity.ActivityCountEntity;
import cn.gzy.domain.activity.model.entity.ActivityEntity;
import cn.gzy.domain.activity.model.entity.ActivitySkuEntity;
import org.springframework.stereotype.Repository;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动仓储接口
 * @create 2024-03-16 10:31
 */
@Repository
public interface IActivityRepository {

    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);

}
