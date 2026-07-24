package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "保存值班安排请求")
public record SysClassDutySaveReq(
        @NotNull(message = "校区不能为空")
        @Positive(message = "校区ID必须大于0")
        Long campusId,

        @Positive(message = "班级ID必须大于0")
        Long classId,

        @NotNull(message = "值班老师不能为空")
        @Positive(message = "老师ID必须大于0")
        Long teacherId,

        @NotNull(message = "值班日期不能为空")
        LocalDate dutyDate,

        @NotBlank(message = "值班类型不能为空")
        String dutyType,

        @Size(max = 255, message = "备注不能超过255个字符")
        String remark
) {
}
