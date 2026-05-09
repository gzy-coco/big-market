package cn.gzy.domain.credit.repository;

import cn.gzy.domain.credit.model.aggregate.TradeAggregate;

public interface ICreditRepository {
    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);
}
