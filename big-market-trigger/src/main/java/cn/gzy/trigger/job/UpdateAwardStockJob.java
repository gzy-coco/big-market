package cn.gzy.trigger.job;

import cn.gzy.domain.strategy.model.entity.StrategyAwardEntity;
import cn.gzy.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.gzy.domain.strategy.service.IRaffleAward;
import cn.gzy.domain.strategy.service.IRaffleStock;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 更新奖品库存任务；为了不让更新库存的压力打到数据库中，这里采用了redis更新缓存库存，异步队列更新数据库，数据库表最终一致即可。
 * @create 2024-02-09 12:13
 */
@Slf4j
@Component()
public class UpdateAwardStockJob {

    @Resource
    private IRaffleAward raffleAward;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private IRaffleStock raffleStock;

    /**
     * 本地化任务注解；@Scheduled(cron = "0/5 * * * * ?")
     * 分布式任务注解； @XxlJob("updateAwardStockJob")
     */
    @XxlJob("updateAwardStockJob")
    public void exec() {
        try {
            List<StrategyAwardStockKeyVO> strategyAwardStockKeyVOS = raffleAward.queryOpenActivityStrategyAwardList();
            if(strategyAwardStockKeyVOS == null) return;
            for(StrategyAwardStockKeyVO strategyAwardStockKeyVO : strategyAwardStockKeyVOS){
                executor.execute(()->{
                    StrategyAwardStockKeyVO queueStrategyAwardStockKeyVO = null;
                    try {
                        queueStrategyAwardStockKeyVO = raffleStock.takeQueueValue(strategyAwardStockKeyVO.getStrategyId(),strategyAwardStockKeyVO.getAwardId());
                        if (null == queueStrategyAwardStockKeyVO) return;
                        log.info("定时任务，更新奖品消耗库存 strategyId:{} awardId:{}", queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                        raffleStock.updateStrategyAwardStock(queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                    } catch (InterruptedException e) {
                        log.error("定时任务，更新奖品消耗库存失败 strategyId:{} awardId:{}", strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
                    }

                });
            }
        } catch (Exception e) {
            log.error("定时任务，更新奖品消耗库存失败", e);
        }
    }

}
