package com.itcjy.emp.pojo.req.homework;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "发布作业请求")
public record HomeworkPublishReq(
        @Schema(description = "作业标题")
        @NotBlank(message = "作业标题不能为空")
        @Size(max = 100, message = "作业标题长度不能超过100个字符")
        String title,

        @Schema(description = "作业Markdown文件对象存储Key")
        @NotBlank(message = "作业文件objectKey不能为空")
        @Size(max = 500, message = "作业文件objectKey长度不能超过500个字符")
        @Pattern(regexp = "(?i)^.+\\.(md|markdown)$", message = "作业文件必须是Markdown文件")
        String contentObjectKey,

        @Schema(description = "作业Markdown文件名")
        @NotBlank(message = "作业文件名不能为空")
        @Size(max = 255, message = "作业文件名长度不能超过255个字符")
        @Pattern(regexp = "(?i)^.+\\.(md|markdown)$", message = "作业文件必须是Markdown文件")
        String contentFileName,

        @Schema(description = "班级ID")
        @NotNull(message = "班级不能为空")
        @Positive(message = "班级ID必须大于0")
        Long classId,

        @Schema(description = "作业日期", example = "2026-07-24")
        @NotNull(message = "作业日期不能为空")
        LocalDate homeworkDate,

        @Schema(description = "开始时间", example = "2026-07-24 00:00:00")
        @NotNull(message = "开始时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        @Schema(description = "截止时间", example = "2026-07-24 23:59:59")
        @NotNull(message = "截止时间不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deadline,

        @Schema(description = "备注")
        @Size(max = 500, message = "备注长度不能超过500个字符")
        String remark
) {
}
