package cn.gzy.domain.award.service;

import cn.gzy.domain.award.event.SendAwardMessageEvent;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordBatchAggregate;
import cn.gzy.domain.award.model.entity.DistributeAwardEntity;
import cn.gzy.domain.award.model.entity.TaskEntity;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;
import cn.gzy.domain.award.model.valobj.TaskStateVO;
import cn.gzy.domain.award.repository.IAwardRepository;
import cn.gzy.domain.award.service.distribute.IDistributeAward;
import cn.gzy.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class AwardService implements IAwardService{

    private final IAwardRepository awardRepository;
    private final SendAwardMessageEvent sendAwardMessageEvent;
    private final Map<String, IDistributeAward> distributeAwardMap;

    public AwardService(IAwardRepository awardRepository, SendAwardMessageEvent sendAwardMessageEvent, Map<String, IDistributeAward> distributeAwardMap) {
        this.awardRepository = awardRepository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
        this.distributeAwardMap = distributeAwardMap;
    }
    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {

        // 构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
        sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());
        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> eventMessage = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

        // 构建任务对象
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUserId(userAwardRecordEntity.getUserId());
        taskEntity.setTopic(sendAwardMessageEvent.topic());
        taskEntity.setMessageId(eventMessage.getId());
        taskEntity.setMessage(eventMessage);
        taskEntity.setState(TaskStateVO.create);

        UserAwardRecordAggregate userAwardRecordAggregate = new UserAwardRecordAggregate();

        userAwardRecordAggregate.setUserAwardRecordEntity(userAwardRecordEntity);
        userAwardRecordAggregate.setTaskEntity(taskEntity);

        awardRepository.saveUserAwardRecord(userAwardRecordAggregate);
    }

    @Override
    public void saveUserAwardRecords(String partakeOrderId, List<UserAwardRecordEntity> userAwardRecordEntities) {
        if (null == userAwardRecordEntities || userAwardRecordEntities.isEmpty()) return;

        // 为每条中奖记录构建一条本地消息任务；发奖消息带各自的子orderId，发奖回写按子号定位互不覆盖
        List<TaskEntity> taskEntities = new ArrayList<>(userAwardRecordEntities.size());
        for (UserAwardRecordEntity userAwardRecordEntity : userAwardRecordEntities) {
            SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
            sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
            sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
            sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
            sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
            sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());
            BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> eventMessage = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUserId(userAwardRecordEntity.getUserId());
            taskEntity.setTopic(sendAwardMessageEvent.topic());
            taskEntity.setMessageId(eventMessage.getId());
            taskEntity.setMessage(eventMessage);
            taskEntity.setState(TaskStateVO.create);
            taskEntities.add(taskEntity);
        }

        UserAwardRecordBatchAggregate batchAggregate = UserAwardRecordBatchAggregate.builder()
                .userId(userAwardRecordEntities.get(0).getUserId())
                .orderId(partakeOrderId)
                .userAwardRecordEntities(userAwardRecordEntities)
                .taskEntities(taskEntities)
                .build();

        awardRepository.saveUserAwardRecords(batchAggregate);
    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        // 奖品Key
        String awardKey = awardRepository.queryAwardKey(distributeAwardEntity.getAwardId());
        if (null == awardKey) {
            log.error("分发奖品，奖品ID不存在。awardKey:{}", awardKey);
            return;
        }

        // 奖品服务
        IDistributeAward distributeAward = distributeAwardMap.get(awardKey);

        if (null == distributeAward) {
            log.error("分发奖品，对应的服务不存在。awardKey:{}", awardKey);
            return;
//            throw new RuntimeException("分发奖品，奖品" + awardKey + "对应的服务不存在");
        }

        // 发放奖品
        distributeAward.giveOutPrizes(distributeAwardEntity);
    }
}
