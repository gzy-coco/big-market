package cn.gzy.test.trigger;

import cn.gzy.trigger.api.IRaffleActivityService;
import cn.gzy.trigger.api.dto.ActivityDrawRequestDTO;
import cn.gzy.trigger.api.dto.ActivityDrawResponseDTO;
import cn.gzy.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
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
        ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
        request.setActivityId(100301L);
        request.setUserId("user003");
        Response<ActivityDrawResponseDTO> response = raffleActivityService.draw(request);

        log.info("请求参数：{}", JSON.toJSONString(request));
        log.info("测试结果：{}", JSON.toJSONString(response));

        new CountDownLatch(1).await();
    }


    public void test_calendarSignRebate() {
//        ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
//        request.setActivityId(100301L);
//        request.setUserId("user003");
        String userId = "user003";
        Response<Boolean> response = raffleActivityService.calendarSignRebate(userId);

        log.info("请求参数：{}", userId);
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

}
