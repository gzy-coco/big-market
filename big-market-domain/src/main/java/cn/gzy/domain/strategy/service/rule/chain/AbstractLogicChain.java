package cn.gzy.domain.strategy.service.rule.chain;

public abstract class AbstractLogicChain implements ILogicChain{

    protected ILogicChain next;
    @Override
    public ILogicChain appendNext(ILogicChain next) {
        this.next = next;
        return next;
    }

    @Override
    public ILogicChain next() {
        return next;
    }

    protected abstract String ruleModel();
}
