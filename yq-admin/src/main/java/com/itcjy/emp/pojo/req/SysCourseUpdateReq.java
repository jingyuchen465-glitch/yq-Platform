package com.itcjy.emp.pojo.req;

import com.itcjy.common.annotations.TeachingMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "课程修改请求参数")
public class SysCourseUpdateReq {

    @Schema(description = "课程名称", example = "Java高级编程")
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 128, message = "课程名称长度不能超过128个字符")
    private String courseName;

    @Schema(description = "课程天数", example = "30")
    @NotNull(message = "课程天数不能为空")
    @Min(value = 1, message = "课程天数必须大于0")
    private Integer courseDays;

    @Schema(description = "上课方式", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE"})
    @NotBlank(message = "上课方式不能为空")
    @TeachingMode
    private String teachingMode;

    @Schema(description = "资料路径", example = "/materials/java-advanced")
    @NotBlank(message = "资料路径不能为空")
    @Size(max = 500, message = "资料路径长度不能超过500个字符")
    private String materialPath;
}
