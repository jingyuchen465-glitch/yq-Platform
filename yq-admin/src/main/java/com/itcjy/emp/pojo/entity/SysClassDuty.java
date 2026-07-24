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
@TableName("sys_class_duty")
@Schema(description = "值班记录实体")
public class SysClassDuty {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long campusId;

    private Long teacherId;

    private LocalDate dutyDate;

    private String dutyType;

    private String startTime;

    private String endTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
