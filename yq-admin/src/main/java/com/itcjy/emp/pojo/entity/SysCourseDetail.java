package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_course_detail")
@Schema(description = "课程详情实体")
public class SysCourseDetail {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "课程详情ID", example = "1")
    private Long id;

    @Schema(description = "课程ID", example = "1")
    private Long courseId;

    @Schema(description = "阶段", example = "Java基础")
    private String stageName;

    @Schema(description = "第几天，线下课程使用", example = "1")
    private Integer dayNumber;

    @Schema(description = "上课内容，线下课程使用", example = "变量与数据类型")
    private String classContent;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
