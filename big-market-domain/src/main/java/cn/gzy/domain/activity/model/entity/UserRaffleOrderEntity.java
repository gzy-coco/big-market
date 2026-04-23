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
}
