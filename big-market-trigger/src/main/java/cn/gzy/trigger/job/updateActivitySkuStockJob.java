package cn.gzy.trigger.job;


import cn.gzy.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.gzy.domain.activity.service.IRaffleActivitySkuStockService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 更新活动sku库存任务
 * @create 2024-03-30 09:52
 */
@Slf4j
@Component()
public class updateActivitySkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService skuStock;

    @Resource
    private ThreadPoolExecutor executor;

    /**
     * 本地化任务注解；@Scheduled(cron = "0/5 * * * * ?")
     * 分布式任务注解；@XxlJob("SendMessageTaskJob")
     */
    @XxlJob("UpdateActivitySkuStockJob")
    public void exec() {
        try {
            List<Long> skus = skuStock.querySkuList();
            for(Long sku : skus){
                executor.execute(()->{
                    ActivitySkuStockKeyVO activitySkuStockKeyVO = null;
                    try {
                        activitySkuStockKeyVO = skuStock.takeQueueValue(sku);
                    } catch (InterruptedException e) {
                        log.error("定时任务，更新活动sku库存失败 sku: {}", sku);
                    }
                    if (null == activitySkuStockKeyVO) return;
                    log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
                    skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
                });

            }
        } catch (Exception e) {
            log.error("定时任务，更新活动sku库存失败", e);
        }
    }
}
