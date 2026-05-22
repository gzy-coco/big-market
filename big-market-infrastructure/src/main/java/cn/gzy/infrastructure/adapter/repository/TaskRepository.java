package cn.gzy.infrastructure.adapter.repository;

import cn.gzy.domain.task.model.entity.TaskEntity;
import cn.gzy.domain.task.repository.ITaskRepository;
import cn.gzy.infrastructure.event.EventPublisher;
import cn.gzy.infrastructure.dao.ITaskDao;
import cn.gzy.infrastructure.dao.po.Task;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 任务服务仓储实现
 * @create 2024-04-06 10:57
 */
@Repository
public class TaskRepository implements ITaskRepository {

    @Resource
    private ITaskDao taskDao;

    @Resource
    private EventPublisher eventPublisher;

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        List<Task> tasks = taskDao.queryNoSendMessageTaskList();
        List<TaskEntity> taskEntities = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUserId(task.getUserId());
            taskEntity.setTopic(task.getTopic());
            taskEntity.setMessageId(task.getMessageId());
            taskEntity.setMessage(task.getMessage());
            taskEntities.add(taskEntity);
        }
        return taskEntities;
    }

    @Override
    public void sendMessage(TaskEntity taskEntity) {
        eventPublisher.publish(taskEntity.getTopic(),taskEntity.getMessage());
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setMessageId(messageId);
        taskDao.updateTaskSendMessageCompleted(task);
    }

    @Override
    public void updateTaskSendMessageFail(String userId, String messageId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setMessageId(messageId);
        taskDao.updateTaskSendMessageFail(task);
    }
}
