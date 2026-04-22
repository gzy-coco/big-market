package cn.gzy.domain.activity.service.quota.rule.impl;

import cn.gzy.domain.activity.model.entity.ActivityCountEntity;
import cn.gzy.domain.activity.model.entity.ActivityEntity;
import cn.gzy.domain.activity.model.entity.ActivitySkuEntity;
import cn.gzy.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.gzy.domain.activity.repository.IActivityRepository;
import cn.gzy.domain.activity.service.armory.IActivityDispatch;
import cn.gzy.domain.activity.service.quota.rule.AbstractActionChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.gzy.types.enums.ResponseCode;
import cn.gzy.types.exception.AppException;
import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品库存规则节点
 * @create 2024-03-23 10:25
 */
@Slf4j
@Component("activity_sku_stock_action")
public class ActivitySkuStockActionChain extends AbstractActionChain {

    @Resource
    private IActivityRepository activityRepository;

    @Resource
    private IActivityDispatch activityDispatch;

    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】开始。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
        // 扣减库存
        boolean status = activityDispatch.subtractionActivitySkuStock(activitySkuEntity.getSku(),activityEntity.getEndDateTime());
        // ture 库存扣减成功
        if(status){
            log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】成功。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
            // 写入延迟队列，延迟消费更新库存记录
            activityRepository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                            .activityId(activitySkuEntity.getActivityId())
                            .sku(activitySkuEntity.getSku())
                            .build());
            return true;
        }
        //库存扣减失败
        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
    }

}
