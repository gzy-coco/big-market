package cn.gzy.infrastructure.adapter.repository;


import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.gzy.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordBatchAggregate;
import cn.gzy.domain.award.model.entity.TaskEntity;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;
import cn.gzy.domain.award.model.entity.UserCreditAwardEntity;
import cn.gzy.domain.award.model.valobj.AccountStatusVO;
import cn.gzy.domain.award.repository.IAwardRepository;
import cn.gzy.infrastructure.dao.*;
import cn.gzy.infrastructure.dao.po.*;
import cn.gzy.infrastructure.event.EventPublisher;
import cn.gzy.infrastructure.redis.IRedisService;
import cn.gzy.types.common.Constants;
import cn.gzy.types.enums.ResponseCode;
import cn.gzy.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 奖品仓储服务
 * @create 2024-04-06 10:09
 */
@Repository
@Slf4j
public class AwardRepository implements IAwardRepository {

    @Resource
    private IAwardDao awardDao;

    @Resource
    private IUserAwardRecordDao userAwardRecordDao;

    @Resource
    private ITaskDao taskDao;

    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private IUserRaffleOrderDao userRaffleOrderDao;

    @Resource
    private IUserCreditAccountDao userCreditAccountDao;

    @Resource
    private IRedisService redisService;

    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;

    @Resource
    private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;


    @Override
    public void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate) {
        TaskEntity taskEntity = userAwardRecordAggregate.getTaskEntity();
        UserAwardRecordEntity userAwardRecordEntity = userAwardRecordAggregate.getUserAwardRecordEntity();

        String userId = userAwardRecordEntity.getUserId();
        Long activityId = userAwardRecordEntity.getActivityId();
        Integer awardId = userAwardRecordEntity.getAwardId();

        UserAwardRecord userAwardRecord = new UserAwardRecord();
        userAwardRecord.setUserId(userAwardRecordEntity.getUserId());
        userAwardRecord.setActivityId(userAwardRecordEntity.getActivityId());
        userAwardRecord.setStrategyId(userAwardRecordEntity.getStrategyId());
        userAwardRecord.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecord.setAwardId(userAwardRecordEntity.getAwardId());
        userAwardRecord.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        userAwardRecord.setAwardTime(userAwardRecordEntity.getAwardTime());
        userAwardRecord.setAwardState(userAwardRecordEntity.getAwardState().getCode());

        Task task = new Task();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());

        UserRaffleOrder userRaffleOrderReq = new UserRaffleOrder();
        userRaffleOrderReq.setUserId(userAwardRecordEntity.getUserId());
        userRaffleOrderReq.setOrderId(userAwardRecordEntity.getOrderId());
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 写入记录
                    userAwardRecordDao.insert(userAwardRecord);
                    // 写入任务
                    taskDao.insert(task);
                    // 更新抽奖单
                    int count = userRaffleOrderDao.updateUserRaffleOrderStateUsed(userRaffleOrderReq);
                    if (1 != count) {
                        status.setRollbackOnly();
                        log.error("写入中奖记录，用户抽奖单已使用过，不可重复抽奖 userId: {} activityId: {} awardId: {}", userId, activityId, awardId);
                        throw new AppException(ResponseCode.ACTIVITY_ORDER_ERROR.getCode(), ResponseCode.ACTIVITY_ORDER_ERROR.getInfo());
                    }

                    // 4. 更新抽奖次数
                    raffleActivityAccountDao.addUsedCount(userId,userAwardRecord.getActivityId(),1);
                    raffleActivityAccountDayDao.addUsedCount(userId,userAwardRecord.getActivityId(), RaffleActivityAccountDay.currentDay(),1);

                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入中奖记录，唯一索引冲突 userId: {} activityId: {} awardId: {}", userId, activityId, awardId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }

        try {
            // 发送消息【在事务外执行，如果失败还有任务补偿】
            eventPublisher.publish(task.getTopic(), task.getMessage());
            // 更新数据库记录，task 任务表
            taskDao.updateTaskSendMessageCompleted(task);
        } catch (Exception e) {
            log.error("写入中奖记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
            taskDao.updateTaskSendMessageFail(task);
        }
    }

    @Override
    public void saveUserAwardRecords(UserAwardRecordBatchAggregate batchAggregate) {
        String userId = batchAggregate.getUserId();
        String orderId = batchAggregate.getOrderId();
        List<UserAwardRecordEntity> userAwardRecordEntities = batchAggregate.getUserAwardRecordEntities();
        List<TaskEntity> taskEntities = batchAggregate.getTaskEntities();

        // 转换 - 中奖记录 PO
        List<UserAwardRecord> userAwardRecords = new ArrayList<>(userAwardRecordEntities.size());
        for (UserAwardRecordEntity entity : userAwardRecordEntities) {
            UserAwardRecord po = new UserAwardRecord();
            po.setUserId(entity.getUserId());
            po.setActivityId(entity.getActivityId());
            po.setStrategyId(entity.getStrategyId());
            po.setOrderId(entity.getOrderId());
            po.setAwardId(entity.getAwardId());
            po.setAwardTitle(entity.getAwardTitle());
            po.setAwardTime(entity.getAwardTime());
            po.setAwardState(entity.getAwardState().getCode());
            userAwardRecords.add(po);
        }
        // 转换 - 本地消息任务 PO
        List<Task> tasks = new ArrayList<>(taskEntities.size());
        for (TaskEntity taskEntity : taskEntities) {
            Task task = new Task();
            task.setUserId(taskEntity.getUserId());
            task.setTopic(taskEntity.getTopic());
            task.setMessageId(taskEntity.getMessageId());
            task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
            task.setState(taskEntity.getState().getCode());
            tasks.add(task);
        }

        UserRaffleOrder userRaffleOrderReq = new UserRaffleOrder();
        userRaffleOrderReq.setUserId(userId);
        userRaffleOrderReq.setOrderId(orderId);
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 1. 批量写入中奖记录
                    userAwardRecordDao.insertBatch(userAwardRecords);
                    // 2. 批量写入本地消息任务
                    taskDao.insertBatch(tasks);
                    // 3. 标记订单已用一次（10条记录共用同一订单，只标记一次）
                    int count = userRaffleOrderDao.updateUserRaffleOrderStateUsed(userRaffleOrderReq);

                    // 4. 增加总抽奖和每日抽奖次数
                    raffleActivityAccountDao.addUsedCount(userId,userAwardRecords.get(0).getActivityId(),10);
                    raffleActivityAccountDayDao.addUsedCount(userId,userAwardRecords.get(0).getActivityId(), RaffleActivityAccountDay.currentDay(),10);

                    if (1 != count) {
                        status.setRollbackOnly();
                        log.error("批量写入中奖记录，抽奖单已使用过，不可重复抽奖 userId:{} orderId:{}", userId, orderId);
                        throw new AppException(ResponseCode.ACTIVITY_ORDER_ERROR.getCode(), ResponseCode.ACTIVITY_ORDER_ERROR.getInfo());
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("批量写入中奖记录，唯一索引冲突 userId:{} orderId:{}", userId, orderId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }

        // 事务外逐条发送MQ（失败由 task 表 + SendMessageTaskJob 补偿）
        for (Task task : tasks) {
            try {
                eventPublisher.publish(task.getTopic(), task.getMessage());
                taskDao.updateTaskSendMessageCompleted(task);
            } catch (Exception e) {
                log.error("批量写入中奖记录，发送MQ消息失败 userId:{} messageId:{}", userId, task.getMessageId());
                taskDao.updateTaskSendMessageFail(task);
            }
        }
    }

    @Override
    public String queryAwardConfig(Integer awardId) {
        return awardDao.queryAwardConfigByAwardId(awardId);
    }

    @Override
    public void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate) {
        UserCreditAwardEntity userCreditAwardEntity = giveOutPrizesAggregate.getUserCreditAwardEntity();
        UserAwardRecordEntity userAwardRecordEntity = giveOutPrizesAggregate.getUserAwardRecordEntity();
        String userId = giveOutPrizesAggregate.getUserId();

        // 更新发奖记录
        UserAwardRecord userAwardRecordReq = new UserAwardRecord();
        userAwardRecordReq.setUserId(userId);
        userAwardRecordReq.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecordReq.setAwardState(userAwardRecordEntity.getAwardState().getCode());

        // 更新用户积分
        UserCreditAccount userCreditAccountReq = new UserCreditAccount();
        userCreditAccountReq.setUserId(userCreditAwardEntity.getUserId());
        userCreditAccountReq.setTotalAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAvailableAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK + userId);
        try {
            lock.lock(3, TimeUnit.SECONDS);
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 更新积分 || 创建积分账户
                    UserCreditAccount userCreditAccountRes = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
                    if (null == userCreditAccountRes) {
                        userCreditAccountDao.insert(userCreditAccountReq);
                    } else {
                        userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                    }

                    // 更新奖品记录
                    int updateAwardCount = userAwardRecordDao.updateAwardRecordCompletedState(userAwardRecordReq);
                    if (0 == updateAwardCount) {
                        log.warn("更新中奖记录，重复更新拦截 userId:{} giveOutPrizesAggregate:{}", userId, JSON.toJSONString(giveOutPrizesAggregate));
                        status.setRollbackOnly();
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("更新中奖记录，唯一索引冲突 userId: {} ", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
            lock.unlock();
        }

    }

    @Override
    public String queryAwardKey(Integer awardId) {
        return awardDao.queryAwardKeyByAwardId(awardId);
    }
}
