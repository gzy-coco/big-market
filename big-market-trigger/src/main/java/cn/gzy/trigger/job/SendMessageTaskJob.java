package cn.gzy.trigger.job;


import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;

import cn.gzy.domain.task.model.entity.TaskEntity;
import cn.gzy.domain.task.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 发送MQ消息任务队列
 *  查询task 表中为成功发送mq消息的，重新发送mq消息，发送成功则更改task表中对应的状态
 * @create 2024-04-06 10:47
 */

@Slf4j
@Component()
public class SendMessageTaskJob {
    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private IDBRouterStrategy dbRouter;


    @Scheduled(cron = "0/5 * * * * ?")
    public void exec(){
        try{
            // 获取分库数量
            int dbCount = dbRouter.dbCount();

            // 逐个库扫描表【每个库一个任务表】
            for(int dbIdx = 1; dbIdx <= dbCount; dbIdx++){
                int finalDbIdx = dbIdx;
                executor.execute(()->{
                    try{
                        //Note 不能写成 bRouter.setDBKey(dbIdx);
                        //Lambda 表达式（或匿名内部类）内部引用的外部局部变量，必须是 final 的，或者是“事实上的常量”（Effectively Final）。
                        dbRouter.setDBKey(finalDbIdx);
                        dbRouter.setTBKey(0);
                        List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
                        if(taskEntities.isEmpty()) return;
                        // 发送MQ消息
                        for (TaskEntity taskEntity : taskEntities) {
                            // 开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                            executor.execute(() -> {
                                try {
                                    taskService.sendMessage(taskEntity);
                                    taskService.updateTaskSendMessageCompleted(taskEntity.getUserId(), taskEntity.getMessageId());
                                } catch (Exception e) {
                                    log.error("定时任务，发送MQ消息失败 userId: {} topic: {}", taskEntity.getUserId(), taskEntity.getTopic());
                                    taskService.updateTaskSendMessageFail(taskEntity.getUserId(), taskEntity.getMessageId());
                                }
                            });
                        }
                    } finally {
                        dbRouter.clear();
                    }
                });
            }
        }
        catch (Exception e){
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        }finally {
            dbRouter.clear();
        }
    }
}
