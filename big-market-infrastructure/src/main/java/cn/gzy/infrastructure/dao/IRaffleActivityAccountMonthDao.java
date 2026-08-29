package cn.gzy.infrastructure.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.gzy.infrastructure.dao.po.RaffleActivityAccountMonth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动账户表-月次数
 * @create 2024-04-03 15:57
 */
@Mapper
public interface IRaffleActivityAccountMonthDao {

    @DBRouter
    RaffleActivityAccountMonth queryActivityAccountMonthByUserId(RaffleActivityAccountMonth raffleActivityAccountMonthReq);

    int updateActivityAccountMonthSubtractionQuota(RaffleActivityAccountMonth raffleActivityAccountMonth);

    void insertActivityAccountMonth(RaffleActivityAccountMonth raffleActivityAccountMonth);

    void addAccountQuota(RaffleActivityAccountMonth raffleActivityAccountMonth);

    /** 月账户按 count 扣减，WHERE month_count_surplus >= count，影响行数=1 表示成功 */
    int updateActivityAccountMonthSubtractionQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("month") String month, @Param("count") Integer count);

}
