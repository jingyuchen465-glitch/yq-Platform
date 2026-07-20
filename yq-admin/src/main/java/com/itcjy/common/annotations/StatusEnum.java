package com.itcjy.common.annotations;

import com.itcjy.common.validator.EnumValueValidator;
import jakarta.validation.Constraint;

import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface StatusEnum  {
    // 默认错误消息提示.
    String message() default "字段值不符合要求";

    //指定要校验的枚举类,比如在字段上写@EnumValue(enumClass = StatusEnum.class)
    Class<? extends Enum<?>> enumClass();


    //分组校验
    Class<?>[] groups() default {};

    //自定义负载信息
    Class<? extends Payload>[] payload() default {};
}
