package cn.gzy.domain.activity.repository;

import cn.gzy.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.gzy.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.gzy.domain.activity.model.entity.*;
import cn.gzy.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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

    void doSaveNoPayOrder(CreateQuotaOrderAggregate createOrderAggregate);


    void doSaveCreditPayOrder(CreateQuotaOrderAggregate createOrderAggregate);
    void cacheActivitySkuStockCount(String cacheKey,Integer stockCount);

    boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime);

    void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    ActivitySkuStockKeyVO takeQueueValue(Long sku);

    void clearQueueValue(Long sku);


    ActivitySkuStockKeyVO takeQueueValue();

    void clearQueueValue();

    void updateActivitySkuStock(Long sku);

    void clearActivitySkuStock(Long sku);

    ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId);
    ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month);
    ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day);
    void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate);

    UserRaffleOrderEntity queryNoUserRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);

    List<ActivitySkuEntity> queryActivitySkuByActivityId(Long activityId);

    Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId);

    ActivityAccountEntity queryUserActivityAccount(String userId,Long activityId);


    Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);

    void updateOrder(DeliveryOrderEntity deliveryOrderEntity);

    UnpaidActivityOrderEntity queryUnpaidActivityOrder(SkuRechargeEntity skuRechargeEntity);

    List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);


    BigDecimal queryUserCreditAccountAmount(String userId);


    List<Long> querySkuList();

}
