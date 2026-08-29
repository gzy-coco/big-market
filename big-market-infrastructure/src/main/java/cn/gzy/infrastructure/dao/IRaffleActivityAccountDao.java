package cn.gzy.infrastructure.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.gzy.infrastructure.dao.po.RaffleActivityAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动账户表
 * @create 2024-03-09 10:05
 */
@Mapper
public interface IRaffleActivityAccountDao {

    void insert(RaffleActivityAccount raffleActivityAccount);

    int updateAccountQuota(RaffleActivityAccount raffleActivityAccount);

    @DBRouter
    RaffleActivityAccount queryActivityAccountByUserId(RaffleActivityAccount raffleActivityAccountReq);

    int updateActivityAccountSubtractionQuota(RaffleActivityAccount raffleActivityAccount);

    int updateActivityAccountMonthSubtractionQuota(RaffleActivityAccount raffleActivityAccount);

    int updateActivityAccountDaySubtractionQuota(RaffleActivityAccount raffleActivityAccount);

    void updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccount raffleActivityAccount);

    void updateActivityAccountDaySurplusImageQuota(RaffleActivityAccount raffleActivityAccount);

    RaffleActivityAccount queryAccountByUserId(RaffleActivityAccount raffleActivityAccount);

    // ===== 10连抽：按 count 批量扣减 =====

    /** 总账户按 count 扣减，WHERE total_count_surplus >= count，影响行数=1 表示成功 */
    int updateActivityAccountSubtractionQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("count") Integer count);

    /** 总账户中月镜像按 count 扣减 */
    void updateActivityAccountMonthSubtractionQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("count") Integer count);

    /** 总账户中日镜像按 count 扣减 */
    void updateActivityAccountDaySubtractionQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("count") Integer count);

    /** 新建月账户时，重置总账户中月镜像为 monthCountSurplus - count */
    void updateActivityAccountMonthSurplusImageQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("monthCountSurplus") Integer monthCountSurplus, @Param("count") Integer count);

    /** 新建日账户时，重置总账户中日镜像为 dayCountSurplus - count */
    void updateActivityAccountDaySurplusImageQuotaByCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("dayCountSurplus") Integer dayCountSurplus, @Param("count") Integer count);

    /** 增加累计真实已抽次数；只在中奖记录成功落库的同一事务内调用 */
    int addUsedCount(@Param("userId") String userId, @Param("activityId") Long activityId, @Param("count") Integer count);

    @DBRouter
    int queryAccountCount(@Param("userId") String userId, @Param("activityId") Long activityId);

}
