package cn.gzy.infrastructure.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.gzy.infrastructure.dao.po.Task;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 任务表，发送MQ
 * @create 2024-04-03 15:57
 */
@Mapper
public interface ITaskDao {

    List<Task> queryNoSendMessageTaskList();

    void insert(Task task);

    /** 批量写入本地消息任务【10连抽】 */
    void insertBatch(List<Task> tasks);

    @DBRouter
    void updateTaskSendMessageCompleted(Task task);

    @DBRouter
    void updateTaskSendMessageFail(Task task);
}
