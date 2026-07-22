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
@TableName("sys_course")
@Schema(description = "课程实体")
public class SysCourse {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "课程ID", example = "1")
    private Long id;

    @Schema(description = "课程名称", example = "Java高级编程")
    private String courseName;

    @Schema(description = "课程天数", example = "30")
    private Integer courseDays;

    @Schema(description = "上课方式：ONLINE线上，OFFLINE线下", example = "ONLINE")
    private String teachingMode;

    @Schema(description = "资料路径", example = "/materials/java-advanced")
    private String materialPath;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
