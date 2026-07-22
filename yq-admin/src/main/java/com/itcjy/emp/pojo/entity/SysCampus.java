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
@TableName("sys_campus")
@Schema(description = "校区实体")
public class SysCampus {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "校区ID", example = "1")
    private Long id;

    @Schema(description = "校区地点", example = "北京市朝阳区")
    private String campusLocation;

    @Schema(description = "负责人", example = "张三")
    private String managerName;

    @Schema(description = "负责人电话", example = "13800138000")
    private String managerPhone;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
