package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
@Schema(description = "角色实体")
public class SysRole {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "角色ID", example = "1")
    private Long id;

    @Schema(description = "角色编码", example = "ADMIN")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
