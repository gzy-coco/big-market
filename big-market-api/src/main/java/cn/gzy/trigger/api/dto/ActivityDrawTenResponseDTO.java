package cn.gzy.trigger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author gzy
 * @description 活动10连抽响应对象（单条中奖结果，10连抽返回其列表）
 * @create 2026-08-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDrawTenResponseDTO implements Serializable {

    /** 本次连抽的第几抽（1~10） */
    private Integer index;
    /** 奖品ID */
    private Integer awardId;
    /** 奖品标题（名称） */
    private String awardTitle;
    /** 排序编号【策略奖品配置的奖品顺序编号】 */
    private Integer awardIndex;

}
