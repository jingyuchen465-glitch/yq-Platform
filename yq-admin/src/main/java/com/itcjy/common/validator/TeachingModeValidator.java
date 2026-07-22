package com.itcjy.common.validator;

import cn.hutool.core.util.StrUtil;
import com.itcjy.common.annotations.TeachingMode;
import com.itcjy.common.myEnum.TeachingModeEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 上课方式校验器：校验字段值是否为合法的 TeachingModeEnum 枚举名称
 */
public class TeachingModeValidator implements ConstraintValidator<TeachingMode, String> {

    @Override
    public void initialize(TeachingMode constraintAnnotation) {
        // 无需额外初始化
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        for (TeachingModeEnum mode : TeachingModeEnum.values()) {
            if (mode.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
