package cn.gzy.infrastructure.dao;


import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import cn.gzy.infrastructure.dao.po.UserCreditOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserCreditOrderDao {

    void insert(UserCreditOrder userCreditOrder);
}
