package cn.gzy.domain.activity.service.quota.rule;

public interface IActionChainArmory {

    IActionChain next();

    IActionChain appendNext(IActionChain next);
}
