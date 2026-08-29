package cn.gzy.domain.award.model.aggregate;

import cn.gzy.domain.award.model.entity.TaskEntity;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author gzy
 * @description 10连抽批量中奖记录聚合；10条中奖记录 + 10条本地消息任务共用同一 orderId，一次事务落库。
 * @create 2026-08-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAwardRecordBatchAggregate {

    /** 用户ID */
    private String userId;
    /** 抽奖订单ID（10条记录共用，用于标记订单已用的幂等） */
    private String orderId;
    /** 中奖记录列表 */
    private List<UserAwardRecordEntity> userAwardRecordEntities;
    /** 待发送发奖消息的本地消息表任务列表（与中奖记录一一对应） */
    private List<TaskEntity> taskEntities;

}
