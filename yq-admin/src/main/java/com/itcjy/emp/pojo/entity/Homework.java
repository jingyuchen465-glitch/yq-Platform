package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("homework")
@Schema(description = "作业实体")
public class Homework {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "作业ID")
    private Long id;

    @Schema(description = "作业标题")
    private String title;

    @Schema(description = "作业内容对象存储Key")
    private String contentObjectKey;

    @Schema(description = "作业内容文件名")
    private String contentFileName;

    @Schema(description = "答案对象存储Key")
    private String answerObjectKey;

    @Schema(description = "答案文件名")
    private String answerFileName;

    @Schema(description = "答案学生是否可见")
    private Boolean answerStudentVisible;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "作业日期")
    private LocalDate homeworkDate;

    @Schema(description = "课程内容")
    private String classContent;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
