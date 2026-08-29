package cn.gzy.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author gzy
 * @description 抽奖类型；单抽 / 十连抽（用于订单表 raffle_type 字段区分单订单承载的抽奖形态）
 * @create 2026-08-24
 */
@Getter
@AllArgsConstructor
public enum RaffleTypeVO {

    single("single",1, "单抽"),
    ten("ten", 10,"十连抽"),
    ;

    private final String code;
    private final Integer count;
    private final String desc;

    public static RaffleTypeVO fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.getCode().equals(code))
                .findFirst()
                .orElse(null); // 找不到时返回 null，也可以用 .orElseThrow() 抛异常
    }
}
