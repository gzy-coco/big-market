package cn.gzy.test.trigger;

import cn.gzy.trigger.api.IRaffleActivityService;
import cn.gzy.trigger.api.dto.ActivityDrawRequestDTO;
import cn.gzy.trigger.api.dto.ActivityDrawResponseDTO;
import cn.gzy.trigger.api.dto.SkuProductResponseDTO;
import cn.gzy.trigger.api.dto.SkuProductShopCartRequestDTO;
import cn.gzy.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动服务测试
 * @create 2024-04-20 11:02
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityControllerTest {

    @Resource
    private IRaffleActivityService raffleActivityService;

    @Test
    public void test_armory() {
        Response<Boolean> response = raffleActivityService.armory(100301L);
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

    @Test
    public void test_draw() throws InterruptedException {
        int[] arr = new int[12];
        int l = arr.length;
        ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
        request.setActivityId(100301L);
        request.setUserId("user003");
        Response<ActivityDrawResponseDTO> response = raffleActivityService.draw(request);

        log.info("请求参数：{}", JSON.toJSONString(request));
        log.info("测试结果：{}", JSON.toJSONString(response));

        new CountDownLatch(1).await();
    }

    @Test
    public void test_calendarSignRebate() {
//        ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
//        request.setActivityId(100301L);
//        request.setUserId("user003");
        String userId = "user003";
        Response<Boolean> response = raffleActivityService.calendarSignRebate(userId);

        log.info("请求参数：{}", userId);
        log.info("测试结果：{}", JSON.toJSONString(response));
    }


    @Test
    public void test_creditPayExchangeSku() throws InterruptedException {

        SkuProductShopCartRequestDTO requestDTO = new SkuProductShopCartRequestDTO();
        requestDTO.setUserId("xiaofuge");
        requestDTO.setSku(9011L);
        Response<Boolean> response = raffleActivityService.creditPayExchangeSku(requestDTO);

        log.info("请求参数：{}", requestDTO);
        log.info("测试结果：{}", JSON.toJSONString(response));

        new CountDownLatch(1).await();
    }

    @Test
    public void test_queryUserCreditAccount() throws InterruptedException {
        String userId = "xiaofuge";
        Response<BigDecimal> response = raffleActivityService.queryUserCreditAccount(userId);

        log.info("请求参数：{}", userId);
        log.info("测试结果：{}", JSON.toJSONString(response));

    }


    @Test
    public void test_querySkuProductListByActivityId() throws InterruptedException {
        Long activityId = 100301L;
        Response<List<SkuProductResponseDTO>> response = raffleActivityService.querySkuProductListByActivityId(activityId);

        log.info("请求参数：{}", activityId);
        log.info("测试结果：{}", JSON.toJSONString(response));

    }

}
