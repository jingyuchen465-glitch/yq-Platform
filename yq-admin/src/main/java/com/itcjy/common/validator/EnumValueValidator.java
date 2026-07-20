package com.itcjy.common.validator;

import cn.hutool.core.util.StrUtil;
import com.itcjy.common.annotations.StatusEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValueValidator implements ConstraintValidator<StatusEnum, String> {

    private Class<? extends Enum<?>> enumClass;

    // 初始化动作, 课获取注解上属性参数.
    @Override
    public void initialize(StatusEnum constraintAnnotation) {
        enumClass = constraintAnnotation.enumClass();
    }

    // 核心校验逻辑,返回true= 校验通过, false = 校验失败.
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            if (enumConstant.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
