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
@TableName("sys_class_schedule")
@Schema(description = "班级课程表实体")
public class SysClassSchedule {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "课表ID")
    private Long id;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "老师ID")
    private Long teacherId;

    @Schema(description = "排课日期")
    private LocalDate scheduleDate;

    @Schema(description = "课程详情ID，上课日使用")
    private Long courseDetailId;

    @Schema(description = "课程内容")
    private String courseContent;

    @Schema(description = "类型：CLASS/SELF_STUDY/REST/HOLIDAY")
    private String classType;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
