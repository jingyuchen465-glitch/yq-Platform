package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_permission")
@Schema(description = "角色权限关联实体")
public class SysRolePermission {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "角色ID", example = "1")
    private Long roleId;

    @Schema(description = "权限ID", example = "1")
    private Long permissionId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}