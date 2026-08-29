package cn.gzy.domain.activity.service.partake;

import cn.gzy.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.gzy.domain.activity.model.entity.ActivityEntity;
import cn.gzy.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.gzy.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.gzy.domain.activity.model.valobj.ActivityStateVO;
import cn.gzy.domain.activity.model.valobj.RaffleTypeVO;
import cn.gzy.domain.activity.repository.IActivityRepository;
import cn.gzy.domain.activity.service.IRaffleActivityPartakeService;
import cn.gzy.types.enums.ResponseCode;
import cn.gzy.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动参与抽奖类
 * @create 2024-04-05 07:53
 */
@Slf4j
public abstract class AbstractRaffleActivityPartake implements IRaffleActivityPartakeService {

    protected final IActivityRepository activityRepository;

    public AbstractRaffleActivityPartake(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }
    @Override
    public UserRaffleOrderEntity createOrder(String userId,Long activityId,String raffleType){
        PartakeRaffleActivityEntity partakeRaffleActivityEntity = new PartakeRaffleActivityEntity();
        partakeRaffleActivityEntity.setActivityId(activityId);
        partakeRaffleActivityEntity.setUserId(userId);
        partakeRaffleActivityEntity.setRaffleType(RaffleTypeVO.fromCode(raffleType));
        return createOrder(partakeRaffleActivityEntity);
    }

    @Override
    public UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity){
        // 0. 基础信息
        String userId = partakeRaffleActivityEntity.getUserId();
        Long activityId = partakeRaffleActivityEntity.getActivityId();
        Date currentDate = new Date();

        // 查询活动
        ActivityEntity activityEntity = activityRepository.queryRaffleActivityByActivityId(activityId);

        // 校验；活动状态
        if(!activityEntity.getState().equals(ActivityStateVO.open)){
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(),ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }
        // 校验；活动日期「开始时间 <- 当前时间 -> 结束时间」
        if (activityEntity.getBeginDateTime().after(currentDate) || activityEntity.getEndDateTime().before(currentDate)) {
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }
        //2. 查询未被使用的活动参与订单记录
        UserRaffleOrderEntity userRaffleOrderEntity = activityRepository.queryNoUserRaffleOrder(partakeRaffleActivityEntity);
        if(userRaffleOrderEntity != null){
            log.info("创建参与活动订单 userId:{} activityId:{} userRaffleOrderEntity:{}", userId, activityId, JSON.toJSONString(userRaffleOrderEntity));
            return userRaffleOrderEntity;
        }
        // 3. 额度账户过滤&返回账户构建对象
        CreatePartakeOrderAggregate createPartakeOrderAggregate = this.doFilterAccount(userId, activityId, currentDate,partakeRaffleActivityEntity.getRaffleType());

        // 4. 构建订单
        UserRaffleOrderEntity userRaffleOrder = this.buildUserRaffleOrder(userId, activityId, currentDate,partakeRaffleActivityEntity.getRaffleType());
        // 给订单设置用户抽奖次数
        userRaffleOrder.setBaseDayCount(createPartakeOrderAggregate.getActivityAccountDayEntity().getUsedCount());
        userRaffleOrder.setBaseTotalCount(createPartakeOrderAggregate.getActivityAccountEntity().getUsedCount());
        // 5. 填充抽奖单实体对象
        createPartakeOrderAggregate.setUserRaffleOrderEntity(userRaffleOrder);

        // 6. 保存聚合对象 - 一个领域内的一个聚合是一个事务操作
        activityRepository.saveCreatePartakeOrderAggregate(createPartakeOrderAggregate);

        // 7. 返回订单信息
        return userRaffleOrder;
    }

    protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date currentDate,RaffleTypeVO raffleType);

    protected abstract UserRaffleOrderEntity buildUserRaffleOrder(String userId, Long activityId, Date currentDate,RaffleTypeVO raffleType);

}
