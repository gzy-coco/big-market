package cn.gzy.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author gzy
 * @description 活动10连抽请求对象
 * @create 2026-08-24
 */
@Data
public class ActivityDrawTenRequestDTO implements Serializable {

    /** 用户ID */
    private String userId;

    /** 活动ID */
    private Long activityId;

}
