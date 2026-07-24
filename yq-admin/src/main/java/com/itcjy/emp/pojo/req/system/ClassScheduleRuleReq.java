package com.itcjy.emp.pojo.req.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ClassScheduleRuleReq(
        @NotEmpty(message = "上课日不能为空") @Size(max = 7, message = "上课日最多7天")
        List<@Min(value = 1, message = "星期值必须在1到7之间") @Max(value = 7, message = "星期值必须在1到7之间") Integer> classDays,
        @NotNull(message = "自习日不能为空") @Size(max = 7, message = "自习日最多7天")
        List<@Min(value = 1, message = "星期值必须在1到7之间") @Max(value = 7, message = "星期值必须在1到7之间") Integer> selfStudyDays,
        @NotNull(message = "休息日不能为空") @Size(max = 7, message = "休息日最多7天")
        List<@Min(value = 1, message = "星期值必须在1到7之间") @Max(value = 7, message = "星期值必须在1到7之间") Integer> restDays,
        @NotNull(message = "节假日休息配置不能为空") Boolean holidayRest
) {
}
