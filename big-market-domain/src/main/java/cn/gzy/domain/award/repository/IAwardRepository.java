package cn.gzy.domain.award.repository;

import cn.gzy.domain.award.model.aggregate.UserAwardRecordAggregate;

public interface IAwardRepository {

    void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);
}
