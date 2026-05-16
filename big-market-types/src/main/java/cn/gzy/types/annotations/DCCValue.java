package cn.gzy.types.annotations;

import java.lang.annotation.*;

/**
 * 注解，动态配置中心
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})  // 作用于字段
@Documented
public @interface DCCValue {

    String value() default "";

}
