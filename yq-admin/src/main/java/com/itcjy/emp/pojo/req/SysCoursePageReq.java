package com.itcjy.emp.pojo.req;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "课程分页查询参数")
public class SysCoursePageReq extends BasePageReq {

    @Schema(description = "课程名称模糊查询", example = "Java")
    @Size(max = 128, message = "课程名称长度不能超过128个字符")
    private String courseName;

    @Schema(description = "上课方式精确查询", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE"})
    private String teachingMode;
}
