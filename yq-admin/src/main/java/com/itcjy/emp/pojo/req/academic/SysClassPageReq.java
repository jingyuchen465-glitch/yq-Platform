package com.itcjy.emp.pojo.req.academic;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "班级分页查询参数")
public class SysClassPageReq extends BasePageReq {

    @Schema(description = "班级期数模糊查询", example = "Java第18期")
    @Size(max = 64, message = "班级期数长度不能超过64个字符")
    private String classPeriod;

    @Schema(description = "班主任ID")
    @Positive(message = "班主任ID必须大于0")
    private Long headTeacherId;

    @Schema(description = "校区ID")
    @Positive(message = "校区ID必须大于0")
    private Long campusId;

    @Schema(description = "课程ID")
    @Positive(message = "课程ID必须大于0")
    private Long courseId;
}
