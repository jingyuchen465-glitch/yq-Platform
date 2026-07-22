package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "课程详情修改请求参数")
public class SysCourseDetailUpdateReq {

    @Schema(description = "阶段", example = "Java基础")
    @NotBlank(message = "阶段不能为空")
    @Size(max = 128, message = "阶段长度不能超过128个字符")
    private String stageName;

    @Schema(description = "第几天，线下课程使用", example = "1")
    @Min(value = 1, message = "天数必须大于0")
    private Integer dayNumber;

    @Schema(description = "上课内容，线下课程使用", example = "变量与数据类型")
    @Size(max = 1000, message = "上课内容长度不能超过1000个字符")
    private String classContent;
}
