package cn.gzy.domain.rebate.service;

import cn.gzy.domain.award.model.valobj.TaskStateVO;
import cn.gzy.domain.rebate.event.SendRebateMessageEvent;
import cn.gzy.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.gzy.domain.rebate.model.entity.BehaviorEntity;
import cn.gzy.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.gzy.domain.rebate.model.entity.TaskEntity;
import cn.gzy.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import cn.gzy.domain.rebate.repository.IRebateRepository;
import cn.gzy.types.common.Constants;
import cn.gzy.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Service
public class BehaviorRebateService implements IBehaviorRebateService{

    @Resource
    private IRebateRepository behaviorRebateRepository;

    @Resource
    private SendRebateMessageEvent sendRebateMessageEvent;

    @Override
    public List<String> createOrder(BehaviorEntity behaviorEntity) {
        List<DailyBehaviorRebateVO> dailyBehaviorRebateVOS = behaviorRebateRepository.queryDailyBehaviorRebateConfig(behaviorEntity.getBehaviorTypeVO().getCode());
        if(dailyBehaviorRebateVOS == null || dailyBehaviorRebateVOS.isEmpty()) return null;
        // 2. 构建聚合对象
        List<String> orderIds = new ArrayList<>();
        List<BehaviorRebateAggregate> behaviorRebateAggregates = new ArrayList<>();
        for(DailyBehaviorRebateVO dailyBehaviorRebateVO:dailyBehaviorRebateVOS){
            // 拼装业务ID；用户ID_返利类型_外部透彻业务ID
            String bizId = behaviorEntity.getUserId() + Constants.UNDERLINE + dailyBehaviorRebateVO.getRebateType() + Constants.UNDERLINE + behaviorEntity.getOutBusinessNo();
            BehaviorRebateOrderEntity behaviorRebateOrderEntity = BehaviorRebateOrderEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .orderId(RandomStringUtils.randomNumeric(12))
                    .behaviorType(dailyBehaviorRebateVO.getBehaviorType())
                    .rebateDesc(dailyBehaviorRebateVO.getRebateDesc())
                    .rebateType(dailyBehaviorRebateVO.getRebateType())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();

            orderIds.add(behaviorRebateOrderEntity.getOrderId());
            // MQ 消息对象
            SendRebateMessageEvent.RebateMessage rebateMessage = SendRebateMessageEvent.RebateMessage.builder()
                    .userId(behaviorEntity.getUserId())
                    .rebateType(dailyBehaviorRebateVO.getBehaviorType())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();
            // 构建事件消息
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> message = sendRebateMessageEvent.buildEventMessage(rebateMessage);
            // 组装任务对象
            TaskEntity task = TaskEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .topic(sendRebateMessageEvent.topic())
                    .messageId(message.getId())
                    .message(message)
                    .state(TaskStateVO.create)
                    .build();
            behaviorRebateAggregates.add(BehaviorRebateAggregate.builder()
                            .behaviorRebateOrderEntity(behaviorRebateOrderEntity)
                            .taskEntity(task)
                            .userId(behaviorEntity.getUserId())
                            .build());

        }

        // 3. 存储聚合对象数据
        behaviorRebateRepository.saveUserRebateRecord(behaviorEntity.getUserId(), behaviorRebateAggregates);

        return orderIds;
    }
}
