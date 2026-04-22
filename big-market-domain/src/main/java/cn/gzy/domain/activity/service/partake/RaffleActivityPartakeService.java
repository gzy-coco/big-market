package cn.gzy.domain.activity.service.partake;

import cn.gzy.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.gzy.domain.activity.model.entity.*;
import cn.gzy.domain.activity.model.valobj.UserRaffleOrderStateVO;
import cn.gzy.domain.activity.repository.IActivityRepository;
import cn.gzy.types.enums.ResponseCode;
import cn.gzy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description
 * @create 2024-04-05 07:53
 */
@Slf4j
@Service
public class RaffleActivityPartakeService extends AbstractRaffleActivityPartake{


    private final SimpleDateFormat dateFormatMonth = new SimpleDateFormat("yyyy-MM");
    private final SimpleDateFormat dateFormatDay = new SimpleDateFormat("yyyy-MM-dd");

    public RaffleActivityPartakeService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

    @Override
    protected CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date currentDate) {
        ActivityAccountEntity activityAccountEntity = activityRepository.queryActivityAccountByUserId(userId,activityId);

        // 额度判断（只判断总剩余额度）
        if(activityAccountEntity == null || activityAccountEntity.getTotalCountSurplus() <= 0){
            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(),ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
        }
        String month = dateFormatMonth.format(currentDate);
        String day = dateFormatDay.format(currentDate);

        ActivityAccountMonthEntity activityAccountMonth = activityRepository.queryActivityAccountMonthByUserId(userId,activityId,month);
        if(activityAccountMonth != null && activityAccountMonth.getMonthCountSurplus() <=0){
            throw new AppException(ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getCode(),ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getInfo());
        }
        // 创建月账户额度；true = 存在月账户、false = 不存在月账户
        boolean isExistAccountMonth = null != activityAccountMonth;
        if( activityAccountMonth == null ){
            activityAccountMonth = new ActivityAccountMonthEntity();
            activityAccountMonth.setUserId(userId);
            activityAccountMonth.setActivityId(activityId);
            activityAccountMonth.setMonth(month);
            activityAccountMonth.setMonthCount(activityAccountEntity.getMonthCount());
            activityAccountMonth.setMonthCountSurplus(activityAccountEntity.getMonthCountSurplus());
        }

        // 查询日账户额度
        ActivityAccountDayEntity activityAccountDayEntity = activityRepository.queryActivityAccountDayByUserId(userId, activityId, day);
        if (null != activityAccountDayEntity && activityAccountDayEntity.getDayCountSurplus() <= 0) {
            throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getInfo());
        }

        // 创建月账户额度；true = 存在月账户、false = 不存在月账户
        boolean isExistAccountDay = null != activityAccountDayEntity;
        if (null == activityAccountDayEntity) {
            activityAccountDayEntity = new ActivityAccountDayEntity();
            activityAccountDayEntity.setUserId(userId);
            activityAccountDayEntity.setActivityId(activityId);
            activityAccountDayEntity.setDay(day);
            activityAccountDayEntity.setDayCount(activityAccountEntity.getDayCount());
            activityAccountDayEntity.setDayCountSurplus(activityAccountEntity.getDayCountSurplus());
        }

        // 构建对象
        CreatePartakeOrderAggregate createPartakeOrderAggregate = new CreatePartakeOrderAggregate();
        createPartakeOrderAggregate.setUserId(userId);
        createPartakeOrderAggregate.setActivityId(activityId);
        createPartakeOrderAggregate.setActivityAccountEntity(activityAccountEntity);
        createPartakeOrderAggregate.setExistAccountMonth(isExistAccountMonth);
        createPartakeOrderAggregate.setActivityAccountMonthEntity(activityAccountMonth);
        createPartakeOrderAggregate.setExistAccountDay(isExistAccountDay);
        createPartakeOrderAggregate.setActivityAccountDayEntity(activityAccountDayEntity);

        return createPartakeOrderAggregate;
    }

    @Override
    protected UserRaffleOrderEntity buildUserRaffleOrder(String userId, Long activityId, Date currentDate) {
        ActivityEntity activityEntity = activityRepository.queryRaffleActivityByActivityId(activityId);
        // 构建订单
        UserRaffleOrderEntity userRaffleOrderEntity = new UserRaffleOrderEntity();
        userRaffleOrderEntity.setUserId(userId);
        userRaffleOrderEntity.setActivityId(activityId);
        userRaffleOrderEntity.setActivityName(activityEntity.getActivityName());
        userRaffleOrderEntity.setStrategyId(activityEntity.getStrategyId());
        userRaffleOrderEntity.setOrderTime(currentDate);
        userRaffleOrderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
        userRaffleOrderEntity.setOrderState(UserRaffleOrderStateVO.create);
        return userRaffleOrderEntity;

    }
}
