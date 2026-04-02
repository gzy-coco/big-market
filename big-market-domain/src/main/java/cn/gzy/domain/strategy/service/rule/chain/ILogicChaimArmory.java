package cn.gzy.domain.strategy.service.rule.chain;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 责任链装配
 * @create 2024-01-20 11:53
 */
public interface ILogicChaimArmory {

    ILogicChain appendNext(ILogicChain next);

    ILogicChain next();
}
