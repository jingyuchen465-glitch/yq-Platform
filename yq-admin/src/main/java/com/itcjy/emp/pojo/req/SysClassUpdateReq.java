package com.itcjy.emp.pojo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "班级修改请求参数")
public class SysClassUpdateReq {

    @Schema(description = "班级期数", example = "Java第18期")
    @NotBlank(message = "班级期数不能为空")
    @Size(max = 64, message = "班级期数长度不能超过64个字符")
    private String classPeriod;

    @Schema(description = "班主任ID", example = "10")
    @NotNull(message = "班主任不能为空")
    @Positive(message = "班主任ID必须大于0")
    private Long headTeacherId;

    @Schema(description = "校区ID", example = "2")
    @NotNull(message = "校区不能为空")
    @Positive(message = "校区ID必须大于0")
    private Long campusId;

    @Schema(description = "课程ID", example = "5")
    @NotNull(message = "课程不能为空")
    @Positive(message = "课程ID必须大于0")
    private Long courseId;
}
