package cn.gzy.domain.activity.model.entity;

import cn.gzy.domain.activity.model.valobj.UserRaffleOrderStateVO;
import lombok.Data;

import java.util.Date;

@Data
public class UserRaffleOrderEntity {
    /** 用户id */
    private String userId;
    /** 活动ID */
    private Long activityId;
    /** 活动名称 */
    private String activityName;
    /** 抽奖策略ID */
    private Long strategyId;
    /** 订单ID */
    private String orderId;
    /** 下单时间 */
    private Date orderTime;
    /** 订单状态；create-创建、used-已使用、cancel-已作废 */
    private UserRaffleOrderStateVO orderState;
    /** 结束时间 */
    private Date endDateTime;
    /** 抽奖类型；single-单抽、ten-十连抽 */
    private String raffleType;

    /** 扣减前当日已消耗次数；作为日计数器冷启动兜底种子，第 i 抽当日次数 = baseDayCount + i */
    private int baseDayCount;
    /** 扣减前累计已消耗次数；作为总计数器冷启动兜底种子，第 i 抽累计次数 = baseTotalCount + i */
    private long baseTotalCount;
}
