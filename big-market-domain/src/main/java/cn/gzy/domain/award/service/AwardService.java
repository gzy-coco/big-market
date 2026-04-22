package cn.gzy.domain.award.service;

import cn.gzy.domain.award.event.SendAwardMessageEvent;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.gzy.domain.award.model.entity.TaskEntity;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;
import cn.gzy.domain.award.model.valobj.TaskStateVO;
import cn.gzy.domain.award.repository.IAwardRepository;
import cn.gzy.types.event.BaseEvent;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class AwardService implements IAwardService{

    @Resource
    private SendAwardMessageEvent sendAwardMessageEvent;

    @Resource
    private IAwardRepository awardRepository;

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {

        // 构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
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
}
