package com.itcjy.common.annotations;

import com.itcjy.common.validator.TeachingModeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 上课方式校验注解，仅允许 ONLINE / OFFLINE
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TeachingModeValidator.class)
public @interface TeachingMode {

    String message() default "上课方式只能是 ONLINE（线上）或 OFFLINE（线下）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
