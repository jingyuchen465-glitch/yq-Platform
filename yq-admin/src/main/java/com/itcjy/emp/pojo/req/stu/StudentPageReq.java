package com.itcjy.emp.pojo.req.stu;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "学员分页查询参数")
public class StudentPageReq extends BasePageReq {

    @Schema(description = "姓名、手机号或邮箱关键字")
    @Size(max = 128, message = "搜索关键字长度不能超过128个字符")
    private String keyword;

    @Schema(description = "班级ID")
    @Positive(message = "班级ID必须大于0")
    private Long classId;

    @Schema(description = "学员状态")
    @Pattern(
            regexp = "^(TEMPORARY|ATSCHOOL|GRADUATE|WITCHDRAWAL)?$",
            message = "学员状态不合法"
    )
    private String status;
}
