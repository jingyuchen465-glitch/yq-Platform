package com.itcjy.emp.pojo.req.homework;

import com.itcjy.common.myEnum.HomeworkDateType;
import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "班级作业详情分页查询")
public class HomeworkStatusPageReq extends BasePageReq {

    @Schema(description = "日期类型：HOMEWORK_DATE-发布日期，DEADLINE_DATE-截止日期")
    @NotNull(message = "日期类型不能为空")
    private HomeworkDateType dateType;

    @Schema(description = "查询日期", example = "2026-07-24")
    @NotNull(message = "查询日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate queryDate;

    @Schema(description = "班级名称模糊查询")
    @Size(max = 64, message = "班级名称不能超过64个字符")
    private String className;
}
