package cn.gzy.domain.credit.repository;

import cn.gzy.domain.credit.model.aggregate.TradeAggregate;
import cn.gzy.domain.credit.model.entity.CreditAccountEntity;

public interface ICreditRepository {
    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);

    CreditAccountEntity queryUserCreditAccount(String userId);
}
