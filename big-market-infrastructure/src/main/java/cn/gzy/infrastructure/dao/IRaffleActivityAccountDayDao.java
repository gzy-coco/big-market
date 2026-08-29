package cn.gzy.infrastructure.dao;


import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.gzy.infrastructure.dao.po.RaffleActivityAccountDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动账户表-日次数
 * @create 2024-04-03 15:56
 */
@Mapper
public interface IRaffleActivityAccountDayDao {

    @DBRouter
    RaffleActivityAccountDay queryActivityAccountDayByUserId(RaffleActivityAccountDay raffleActivityAccountDayReq);

    int updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDay raffleActivityAccountDay);

    void insertActivityAccountDay(RaffleActivityAccountDay raffleActivityAccountDay);

    @DBRouter
    Integer queryRaffleActivityAccountDayPartakeCount(RaffleActivityAccountDay raffleActivityAccountDay);

    void addAccountQuota(RaffleActivityAccountDay raffleActivityAccountDay);

    /** 日账户按 count 扣减，WHERE day_count_surplus >= count，影响行数=1 表示成功 */
    int updateActivityAccountDaySubtractionQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("day") String day, @Param("count") Integer count);

    /** 增加当日真实已抽次数；只在中奖记录成功落库的同一事务内调用 */
    int addUsedCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("day") String day, @Param("count") Integer count);

    @DBRouter
    RaffleActivityAccountDay queryAccountDayCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("day") String day);

}
