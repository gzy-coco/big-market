package cn.gzy.domain.activity.model.entity;


import cn.gzy.domain.activity.model.valobj.RaffleTypeVO;
import lombok.Data;
/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 参与抽奖活动实体对象
 * @create 2024-04-04 20:02
 */
@Data
public class PartakeRaffleActivityEntity {
    /**
     * 活动ID
     */
    private String userId;
    /**
     * 活动ID
     */
    private Long activityId;

    private RaffleTypeVO raffleType;
}
