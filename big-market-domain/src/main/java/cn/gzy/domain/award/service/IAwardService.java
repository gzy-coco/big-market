package cn.gzy.domain.award.service;


import cn.gzy.domain.award.model.entity.DistributeAwardEntity;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 奖品服务接口
 * @create 2024-04-06 09:03
 */
public interface IAwardService {

    void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity);

    /**
     * 批量保存中奖记录【10连抽】；10条记录各自携带唯一子orderId，一次事务批量落库，
     * 并用批次号（参与订单号）标记参与订单已用一次。
     *
     * @param partakeOrderId          参与订单号（批次号），用于标记参与订单 used
     * @param userAwardRecordEntities 中奖记录列表（每条 orderId 为子号 批次号_序号）
     */
    void saveUserAwardRecords(String partakeOrderId, List<UserAwardRecordEntity> userAwardRecordEntities);

    /**
     * 配送发货奖品
     */
    void distributeAward(DistributeAwardEntity distributeAwardEntity);
}
