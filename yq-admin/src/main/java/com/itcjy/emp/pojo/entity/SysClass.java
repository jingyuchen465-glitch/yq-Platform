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
@TableName("sys_class")
@Schema(description = "班级实体")
public class SysClass {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "班级ID", example = "1")
    private Long id;

    @Schema(description = "班级期数", example = "Java第18期")
    private String classPeriod;

    @Schema(description = "班主任ID", example = "10")
    private Long headTeacherId;

    @Schema(description = "校区ID", example = "2")
    private Long campusId;

    @Schema(description = "课程ID", example = "5")
    private Long courseId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
