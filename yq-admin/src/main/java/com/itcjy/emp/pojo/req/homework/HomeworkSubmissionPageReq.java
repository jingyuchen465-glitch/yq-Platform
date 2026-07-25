package com.itcjy.emp.pojo.req.homework;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "作业提交分页查询")
public class HomeworkSubmissionPageReq extends BasePageReq {

    @Schema(description = "学生姓名或账号")
    @Size(max = 64, message = "学生关键字不能超过64个字符")
    private String studentKeyword;

    @Schema(description = "是否已批改；不传表示全部")
    private Boolean reviewed;

    @Schema(description = "是否逾期；不传表示全部")
    private Boolean lateSubmitted;
}
