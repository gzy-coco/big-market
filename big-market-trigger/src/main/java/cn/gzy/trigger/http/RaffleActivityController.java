package cn.gzy.trigger.http;

import cn.gzy.domain.activity.model.entity.*;
import cn.gzy.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.gzy.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.gzy.domain.activity.service.IRaffleActivityPartakeService;
import cn.gzy.domain.activity.service.IRaffleActivitySkuProductService;
import cn.gzy.domain.activity.service.armory.IActivityArmory;
import cn.gzy.domain.award.model.entity.UserAwardRecordEntity;
import cn.gzy.domain.award.model.valobj.AwardStateVO;
import cn.gzy.domain.award.service.IAwardService;
import cn.gzy.domain.credit.model.entity.CreditAccountEntity;
import cn.gzy.domain.credit.model.entity.TradeEntity;
import cn.gzy.domain.credit.model.valobj.TradeNameVO;
import cn.gzy.domain.credit.model.valobj.TradeTypeVO;
import cn.gzy.domain.credit.service.ICreditAdjustService;
import cn.gzy.domain.rebate.model.entity.BehaviorEntity;
import cn.gzy.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.gzy.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.gzy.domain.rebate.service.IBehaviorRebateService;
import cn.gzy.domain.strategy.model.entity.RaffleAwardEntity;
import cn.gzy.domain.strategy.model.entity.RaffleFactorEntity;
import cn.gzy.domain.strategy.service.IRaffleStrategy;
import cn.gzy.domain.strategy.service.armory.IStrategyArmory;
import cn.gzy.trigger.api.IRaffleActivityService;
import cn.gzy.trigger.api.dto.*;
import cn.gzy.types.annotations.DCCValue;
import cn.gzy.types.annotations.RateLimiterAccessInterceptor;
import cn.gzy.types.common.Constants;
import cn.gzy.types.enums.ResponseCode;
import cn.gzy.types.exception.AppException;
import cn.gzy.types.model.Response;
import com.alibaba.fastjson.JSON;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
//import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/activity/")
//@DubboService(version = "1.0")
public class RaffleActivityController implements IRaffleActivityService {
    private final SimpleDateFormat dateFormatDay = new SimpleDateFormat("yyyyMMdd");
    @Resource
    private IActivityArmory activityArmory;

    @Resource
    private IStrategyArmory strategyArmory;

    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;

    @Resource
    private IRaffleStrategy raffleStrategy;

    @Resource
    private IAwardService awardService;

    @Resource
    private IBehaviorRebateService behaviorRebateService;

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @Resource
    private ICreditAdjustService creditAdjustService;

    @Resource
    private IRaffleActivitySkuProductService raffleActivitySkuProductService;

    // 10连抽：并行执行抽奖的线程池
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    // dcc 统一配置中心动态配置降级开关
    @DCCValue("degradeSwitch:close")
    private String degradeSwitch;
    /**
     * 活动装配 - 数据预热 | 把活动配置的对应的 sku 一起装配
     *
     * @param activityId 活动ID
     * @return 装配结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/armory">/api/v1/raffle/activity/armory</a>
     * 入参：{"activityId":100001,"userId":"xiaofuge"}
     *
     * curl --request GET \
     *   --url 'http://localhost:8091/api/v1/raffle/activity/armory?activityId=100301'
     */
    @RequestMapping(value = "armory", method = RequestMethod.GET)
    @Override
    public Response<Boolean> armory(@RequestParam Long activityId) {

        try{
            log.info("活动装配，数据预热，开始 activityId:{}", activityId);
            // 1. 活动装配
            activityArmory.assembleActivitySkuByActivityId(activityId);
            // 2. 策略装配
            strategyArmory.assembleLotteryStrategyByActivityId(activityId);
            Response<Boolean> response = Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
            log.info("活动装配，数据预热，完成 activityId:{}", activityId);
            return response;
        }catch (Exception e){
            log.error("活动装配，数据预热，失败 activityId:{}", activityId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 抽奖接口
     *
     * @param request 请求对象
     * @return 抽奖结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/draw">/api/v1/raffle/activity/draw</a>
     * 入参：{"activityId":100001,"userId":"xiaofuge"}
     * <p>
     * curl --request POST \
     * --url http://localhost:8091/api/v1/raffle/activity/draw \
     * --header 'content-type: application/json' \
     * --data '{
     * "userId":"xiaofuge",
     * "activityId": 100301
     * }'
     * 限流配置
     * RateLimiterAccessInterceptor
     * key: 以用户ID作为拦截，这个用户访问次数限制
     * fallbackMethod：失败后的回调方法，方法出入参保持一样
     * permitsPerSecond：每秒的访问频次限制
     * blacklistCount：超过多少次都被限制了，还访问的，扔到黑名单里24小时
     */
//    @RateLimiterAccessInterceptor(key = "userId", fallbackMethod = "drawRateLimiterError", permitsPerSecond = 1.0d, blacklistCount = 1)
//    @HystrixCommand(commandProperties = {
//            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "150")
//    }, fallbackMethod = "drawHystrixError"
//    )
    @RequestMapping(value = "draw", method = RequestMethod.POST)
    @Override
    public Response<ActivityDrawResponseDTO> draw(@RequestBody ActivityDrawRequestDTO request) {
        try{
            log.info("活动抽奖 userId:{} activityId:{}", request.getUserId(), request.getActivityId());

            // 0. 降级开关【open 开启、close 关闭】
            if (StringUtils.isNotBlank(degradeSwitch) && "open".equals(degradeSwitch)) {
                return Response.<ActivityDrawResponseDTO>builder()
                        .code(ResponseCode.DEGRADE_SWITCH.getCode())
                        .info(ResponseCode.DEGRADE_SWITCH.getInfo())
                        .build();
            }

            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 参与活动 - 创建参与记录订单
            UserRaffleOrderEntity userRaffleOrderEntity = raffleActivityPartakeService.createOrder(request.getUserId(),request.getActivityId(),Constants.SINGLE_RAFFLE);
            log.info("活动抽奖，创建订单 userId:{} activityId:{} orderId:{}", request.getUserId(), request.getActivityId(), userRaffleOrderEntity.getOrderId());
            RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                            .userId(userRaffleOrderEntity.getUserId())
                            .strategyId(userRaffleOrderEntity.getStrategyId())
                            .endDateTime(userRaffleOrderEntity.getEndDateTime())
                            .todayUserRaffleCount(userRaffleOrderEntity.getBaseDayCount())
                            .totalUserRaffleCount(userRaffleOrderEntity.getBaseTotalCount())
                            .build());

            // 4. 存放结果 - 写入中奖记录
            UserAwardRecordEntity userAwardRecord = UserAwardRecordEntity.builder()
                    .userId(userRaffleOrderEntity.getUserId())
                    .activityId(userRaffleOrderEntity.getActivityId())
                    .strategyId(userRaffleOrderEntity.getStrategyId())
                    .orderId(userRaffleOrderEntity.getOrderId())
                    .awardId(raffleAwardEntity.getAwardId())
                    .awardTitle(raffleAwardEntity.getAwardTitle())
                    .awardTime(new Date())
                    .awardState(AwardStateVO.create)
                    .awardConfig(raffleAwardEntity.getAwardConfig())
                    .build();

            awardService.saveUserAwardRecord(userAwardRecord);
            // 5. 返回结果
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ActivityDrawResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardTitle(raffleAwardEntity.getAwardTitle())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
        } catch (AppException e) {
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 活动10连抽接口
     * <p>
     * curl --request POST \
     * --url http://localhost:8091/api/v1/raffle/activity/draw_ten \
     * --header 'content-type: application/json' \
     * --data '{"userId":"xiaofuge","activityId": 100301}'
     */
    @RateLimiterAccessInterceptor(key = "userId", fallbackMethod = "drawRateLimiterError", permitsPerSecond = 1.0d, blacklistCount = 1)
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000")
    }, fallbackMethod = "drawHystrixError"
    )
    @RequestMapping(value = "draw_ten", method = RequestMethod.POST)
    @Override
    public Response<List<ActivityDrawTenResponseDTO>> drawTen(@RequestBody ActivityDrawTenRequestDTO request) {
        try {
            log.info("活动10连抽 userId:{} activityId:{}", request.getUserId(), request.getActivityId());

            // 0. 降级开关【open 开启、close 关闭】
            if (StringUtils.isNotBlank(degradeSwitch) && "open".equals(degradeSwitch)) {
                return Response.<List<ActivityDrawTenResponseDTO>>builder()
                        .code(ResponseCode.DEGRADE_SWITCH.getCode())
                        .info(ResponseCode.DEGRADE_SWITCH.getInfo())
                        .build();
            }

            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            final String userId = request.getUserId();
            final Long activityId = request.getActivityId();

            // 2. 批量参与记账：一次事务扣总/月/日各10并写1条订单（额度不足整单拒绝），返回扣减前基数
            UserRaffleOrderEntity userRaffleOrderEntity = raffleActivityPartakeService.createOrder(request.getUserId(),request.getActivityId(),Constants.TEN_RAFFLE);

            final Long strategyId = userRaffleOrderEntity.getStrategyId();
            final Date endDateTime = userRaffleOrderEntity.getEndDateTime();
            final String orderId = userRaffleOrderEntity.getOrderId();
            final int baseDay = userRaffleOrderEntity.getBaseDayCount();
            final long baseTotal = userRaffleOrderEntity.getBaseTotalCount();
            log.info("活动10连抽，创建订单 userId:{} activityId:{} orderId:{}", userId, activityId, orderId);

            // 3. 线程池并行抽10次；每抽的"日/累计次数"直接由创建订单时查得的DB基数 + 序号注入
            //    - 次数在创建订单(单线程)时从DB查得 baseDay/baseTotal，第i抽注入 base+i+1，各不相同、无需查Redis、无锁冲突
            //    - 日次数给 rule_lock；累计次数给保底责任链 rule_guaranteed
            //    - 次数用DB真值而非Redis自增：次数要求强准确(不多不少)，失败也无需回滚Redis
            List<Callable<RaffleAwardEntity>> tasks = new ArrayList<>(10);
            for (int i = 0; i < 10; i++) {
                final int index = i;
                tasks.add(() -> {
                    int daySeq = baseDay + index + 1;
                    long totalSeq = baseTotal + index + 1;
                    return raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                            .userId(userId)
                            .strategyId(strategyId)
                            .endDateTime(endDateTime)
                            .todayUserRaffleCount(daySeq)
                            .totalUserRaffleCount(totalSeq)
                            .build());
                });
            }
            List<Future<RaffleAwardEntity>> futures = threadPoolExecutor.invokeAll(tasks);

            // 4. 收集结果（Future 顺序 == 提交顺序，避免共享集合的线程不安全）
            List<RaffleAwardEntity> raffleAwardEntities = new ArrayList<>(10);
            for (Future<RaffleAwardEntity> future : futures) {
                try {
                    raffleAwardEntities.add(future.get());
                } catch (ExecutionException ee) {
                    // 透出子任务里的业务异常码；其余异常整单失败走补偿
                    if (ee.getCause() instanceof AppException) throw (AppException) ee.getCause();
                    throw new RuntimeException(ee.getCause());
                }
            }

            // 5. 组装10条中奖记录，批量落库（一次事务插10记录+10任务、参与订单标记used一次）
            //    B1方案：参与订单仍1条（批次号=orderId）；中奖记录各派生唯一子orderId = 批次号_序号，
            //    使"1条中奖记录 = 1个唯一orderId"重新成立，发奖回写/幂等按子号定位，即便中相同奖也互不覆盖。
            Date awardTime = new Date();
            List<UserAwardRecordEntity> userAwardRecords = new ArrayList<>(10);
            for (int i = 0; i < raffleAwardEntities.size(); i++) {
                RaffleAwardEntity raffleAwardEntity = raffleAwardEntities.get(i);
                // 子orderId：批次号_两位序号（01~10）
                String subOrderId = orderId + Constants.UNDERLINE + String.format("%02d", i + 1);
                userAwardRecords.add(UserAwardRecordEntity.builder()
                        .userId(userId)
                        .activityId(activityId)
                        .strategyId(strategyId)
                        .orderId(subOrderId)
                        .awardId(raffleAwardEntity.getAwardId())
                        .awardTitle(raffleAwardEntity.getAwardTitle())
                        .awardTime(awardTime)
                        .awardState(AwardStateVO.create)
                        .awardConfig(raffleAwardEntity.getAwardConfig())
                        .build());
            }
            // 批次号用于标记参与订单used（参与订单存的是批次号，非子号）
            awardService.saveUserAwardRecords(orderId, userAwardRecords);

            // 6. 组装返回
            List<ActivityDrawTenResponseDTO> data = new ArrayList<>(raffleAwardEntities.size());
            for (int i = 0; i < raffleAwardEntities.size(); i++) {
                RaffleAwardEntity raffleAwardEntity = raffleAwardEntities.get(i);
                data.add(ActivityDrawTenResponseDTO.builder()
                        .index(i + 1)
                        .awardId(raffleAwardEntity.getAwardId())
                        .awardTitle(raffleAwardEntity.getAwardTitle())
                        .awardIndex(raffleAwardEntity.getSort())
                        .build());
            }
            log.info("活动10连抽完成 userId:{} activityId:{} orderId:{}", userId, activityId, orderId);
            return Response.<List<ActivityDrawTenResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(data)
                    .build();
        } catch (AppException e) {
            log.error("活动10连抽失败 userId:{} activityId:{} {}", request.getUserId(), request.getActivityId(), e.getInfo());
            return Response.<List<ActivityDrawTenResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("活动10连抽失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<List<ActivityDrawTenResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


    public Response<ActivityDrawResponseDTO> drawRateLimiterError(@RequestBody ActivityDrawRequestDTO request) {
        log.info("活动抽奖限流 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
        return Response.<ActivityDrawResponseDTO>builder()
                .code(ResponseCode.RATE_LIMITER.getCode())
                .info(ResponseCode.RATE_LIMITER.getInfo())
                .build();
    }

    public Response<ActivityDrawResponseDTO> drawHystrixError(@RequestBody ActivityDrawRequestDTO request) {
        log.info("活动抽奖熔断 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
        return Response.<ActivityDrawResponseDTO>builder()
                .code(ResponseCode.HYSTRIX.getCode())
                .info(ResponseCode.HYSTRIX.getInfo())
                .build();
    }

    /**
     * 日历签到返利接口
     *
     * @param userId 用户ID
     * @return 签到返利结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/calendar_sign_rebate">/api/v1/raffle/activity/calendar_sign_rebate</a>
     * 入参：xiaofuge
     * <p>
     * curl -X POST http://localhost:8091/api/v1/raffle/activity/calendar_sign_rebate -d "userId=xiaofuge" -H "Content-Type: application/x-www-form-urlencoded"
     */
    @Override
    @RequestMapping(value = "calendar_sign_rebate", method = RequestMethod.POST)
    public Response<Boolean> calendarSignRebate(@RequestParam String userId) {
        try {
            log.info("日历签到返利开始 userId:{}", userId);
            BehaviorEntity behaviorEntity = new BehaviorEntity();
            behaviorEntity.setUserId(userId);
            behaviorEntity.setBehaviorTypeVO(BehaviorTypeVO.SIGN);
            behaviorEntity.setOutBusinessNo(dateFormatDay.format(new Date()));
            List<String> orderIds = behaviorRebateService.createOrder(behaviorEntity);
            log.info("日历签到返利完成 userId:{} orderIds: {}", userId, JSON.toJSONString(orderIds));
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (AppException e) {
            log.error("日历签到返利异常 userId:{} ", userId, e);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("日历签到返利失败 userId:{}", userId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    /**
     * 判断是否签到接口
     * <p>
     * curl -X POST http://localhost:8091/api/v1/raffle/activity/is_calendar_sign_rebate -d "userId=xiaofuge" -H "Content-Type: application/x-www-form-urlencoded"
     */
    @Override
    @RequestMapping(value = "is_calendar_sign_rebate", method = RequestMethod.POST)
    public Response<Boolean> isCalendarSignRebate(@RequestParam String userId) {
        try{
            log.info("查询用户是否完成日历签到返利开始 userId:{}", userId);
            String outBusinessNo = dateFormatDay.format(new Date());
            List<BehaviorRebateOrderEntity> behaviorRebateOrderEntities = behaviorRebateService.queryOrderByOutBusinessNo(userId,outBusinessNo);
            log.info("查询用户是否完成日历签到返利完成 userId:{} orders.size:{}", userId, behaviorRebateOrderEntities.size());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(!behaviorRebateOrderEntities.isEmpty())
                    .build();
        }catch(Exception e){
            log.error("查询用户是否完成日历签到返利失败 userId:{}", userId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }

    }
    /**
     * 查询账户额度
     * <p>
     * curl --request POST \
     * --url http://localhost:8091/api/v1/raffle/activity/query_user_activity_account \
     * --header 'content-type: application/json' \
     * --data '{
     * "userId":"xiaofuge",
     * "activityId": 100301
     * }'
     */
    @Override
    @RequestMapping(value = "query_user_activity_account", method = RequestMethod.POST)
    public Response<UserActivityAccountResponseDTO> queryUserActivityAccount(@RequestBody UserActivityAccountRequestDTO request) {
        try{
            log.info("查询用户活动账户开始 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            ActivityAccountEntity activityAccountEntity = raffleActivityAccountQuotaService.queryUserActivityAccount(request.getUserId(),request.getActivityId());
            UserActivityAccountResponseDTO userActivityAccountResponseDTO = UserActivityAccountResponseDTO.builder()
                    .totalCount(activityAccountEntity.getTotalCount())
                    .totalCountSurplus(activityAccountEntity.getTotalCountSurplus())
                    .dayCount(activityAccountEntity.getDayCount())
                    .dayCountSurplus(activityAccountEntity.getDayCountSurplus())
                    .monthCount(activityAccountEntity.getMonthCount())
                    .monthCountSurplus(activityAccountEntity.getMonthCountSurplus())
                    .build();
            log.info("查询用户活动账户完成 userId:{} activityId:{} dto:{}", request.getUserId(), request.getActivityId(), JSON.toJSONString(userActivityAccountResponseDTO));
            return Response.<UserActivityAccountResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(userActivityAccountResponseDTO)
                    .build();
        }catch (Exception e){
            log.error("查询用户活动账户失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<UserActivityAccountResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "query_sku_product_list_by_activity_id", method = RequestMethod.POST)
    @Override
    public Response<List<SkuProductResponseDTO>> querySkuProductListByActivityId(Long activityId) {
        try {
            log.info("查询sku商品集合开始 activityId:{}", activityId);
            // 1. 参数校验
            if (null == activityId) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询商品&封装数据
            List<SkuProductEntity> skuProductEntities = raffleActivitySkuProductService.querySkuProductEntityListByActivityId(activityId);
            List<SkuProductResponseDTO> skuProductResponseDTOS = new ArrayList<>(skuProductEntities.size());
            for (SkuProductEntity skuProductEntity : skuProductEntities) {

                SkuProductResponseDTO.ActivityCount activityCount = new SkuProductResponseDTO.ActivityCount();
                activityCount.setTotalCount(skuProductEntity.getActivityCount().getTotalCount());
                activityCount.setMonthCount(skuProductEntity.getActivityCount().getMonthCount());
                activityCount.setDayCount(skuProductEntity.getActivityCount().getDayCount());

                SkuProductResponseDTO skuProductResponseDTO = new SkuProductResponseDTO();
                skuProductResponseDTO.setSku(skuProductEntity.getSku());
                skuProductResponseDTO.setActivityId(skuProductEntity.getActivityId());
                skuProductResponseDTO.setActivityCountId(skuProductEntity.getActivityCountId());
                skuProductResponseDTO.setStockCount(skuProductEntity.getStockCount());
                skuProductResponseDTO.setStockCountSurplus(skuProductEntity.getStockCountSurplus());
                skuProductResponseDTO.setProductAmount(skuProductEntity.getProductAmount());
                skuProductResponseDTO.setActivityCount(activityCount);
                skuProductResponseDTOS.add(skuProductResponseDTO);
            }

            log.info("查询sku商品集合完成 activityId:{} skuProductResponseDTOS:{}", activityId, JSON.toJSONString(skuProductResponseDTOS));
            return Response.<List<SkuProductResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(skuProductResponseDTOS)
                    .build();
        } catch (Exception e) {
            log.error("查询sku商品集合失败 activityId:{}", activityId, e);
            return Response.<List<SkuProductResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @RequestMapping(value = "query_user_credit_account", method = RequestMethod.POST)
    public Response<BigDecimal> queryUserCreditAccount(String userId) {
        try{
            log.info("查询用户积分值开始 userId:{}", userId);
            CreditAccountEntity creditAccountEntity = creditAdjustService.queryUserCreditAccount(userId);
            log.info("查询用户积分值完成 userId:{} adjustAmount:{}", userId, creditAccountEntity.getAdjustAmount());
            return Response.<BigDecimal>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(creditAccountEntity.getAdjustAmount())
                    .build();
        }catch (Exception e) {
            log.error("查询用户积分值失败 userId:{}", userId, e);
            return Response.<BigDecimal>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }

    }

    @Override
    @RequestMapping(value = "credit_pay_exchange_sku", method = RequestMethod.POST)
    public Response<Boolean> creditPayExchangeSku(@RequestBody SkuProductShopCartRequestDTO request) {
        try{
            SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
            skuRechargeEntity.setSku(request.getSku());
            skuRechargeEntity.setUserId(request.getUserId());
            skuRechargeEntity.setOutBusinessNo(RandomStringUtils.randomNumeric(12));
            skuRechargeEntity.setOrderTradeType(OrderTradeTypeVO.credit_pay_trade);
            UnpaidActivityOrderEntity unpaidActivityOrder = raffleActivityAccountQuotaService.createSkuRechargeOrder(skuRechargeEntity);

            log.info("积分兑换商品，创建订单完成 userId:{} sku:{} outBusinessNo:{}", request.getUserId(), request.getSku(), unpaidActivityOrder.getOutBusinessNo());
            TradeEntity tradeEntity = new TradeEntity();
            tradeEntity.setUserId(unpaidActivityOrder.getUserId());
            tradeEntity.setOutBusinessNo(unpaidActivityOrder.getOutBusinessNo());
            tradeEntity.setTradeName(TradeNameVO.CONVERT_SKU);
            tradeEntity.setTradeType(TradeTypeVO.REVERSE);
            tradeEntity.setAmount(unpaidActivityOrder.getPayAmount().negate());
            String orderId = creditAdjustService.createOrder(tradeEntity);

            log.info("积分兑换商品，支付订单完成  userId:{} sku:{} orderId:{}", request.getUserId(), request.getSku(), orderId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        }
        catch (AppException e) {
            log.error("积分兑换商品失败 userId:{} activityId:{}", request.getUserId(), request.getSku(), e);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        }catch (Exception e){
            log.error("积分兑换商品失败 userId:{} sku:{}", request.getUserId(), request.getSku(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }

    }
}
