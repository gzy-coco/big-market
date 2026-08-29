package cn.gzy.domain.award.repository;

import cn.gzy.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.gzy.domain.award.model.aggregate.UserAwardRecordBatchAggregate;

public interface IAwardRepository {

    void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);

    /**
     * 批量保存中奖记录【10连抽】；一次事务批量插入中奖记录与本地消息任务，并标记订单已用一次。
     */
    void saveUserAwardRecords(UserAwardRecordBatchAggregate userAwardRecordBatchAggregate);

    String queryAwardConfig(Integer awardId);

    void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate);

    String queryAwardKey(Integer awardId);
}
