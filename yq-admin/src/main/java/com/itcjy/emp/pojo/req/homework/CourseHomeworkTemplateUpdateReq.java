package com.itcjy.emp.pojo.req.homework;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourseHomeworkTemplateUpdateReq(
        @NotNull(message = "课程ID不能为空")
        @Positive(message = "课程ID必须大于0")
        Long courseId,

        @NotNull(message = "课程阶段/天次不能为空")
        @Positive(message = "课程详情ID必须大于0")
        Long courseDetailId,

        @NotBlank(message = "作业标准文件对象Key不能为空")
        @Size(max = 500, message = "作业标准文件对象Key不能超过500个字符")
        String contentObjectKey,

        @NotBlank(message = "作业标准文件名不能为空")
        @Size(max = 255, message = "作业标准文件名不能超过255个字符")
        String contentFileName,

        @NotNull(message = "状态不能为空")
        @StatusEnum(enumClass = ActiveEnum.class, message = "状态只能是ACTIVE或INACTIVE")
        String status,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
) {
}
