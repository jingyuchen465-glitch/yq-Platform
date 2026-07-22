package com.itcjy.emp.pojo.req;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "课程详情分页查询参数")
public class SysCourseDetailPageReq extends BasePageReq {

    @Schema(description = "课程ID", example = "1")
    @NotNull(message = "课程ID不能为空")
    @Min(value = 1, message = "课程ID必须大于0")
    private Long courseId;

    @Schema(description = "阶段模糊查询", example = "Java")
    @Size(max = 128, message = "阶段长度不能超过128个字符")
    private String stageName;
}
