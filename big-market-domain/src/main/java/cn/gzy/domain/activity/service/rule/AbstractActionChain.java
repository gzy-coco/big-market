package cn.gzy.domain.activity.service.rule;

public abstract class AbstractActionChain implements IActionChain{
    private IActionChain next;

    public IActionChain next(){
        return this.next;
    }

    public IActionChain appendNext(IActionChain next){
        this.next = next;
        return this.next;
    }

}
