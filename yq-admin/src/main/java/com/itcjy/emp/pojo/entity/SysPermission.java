package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
@Schema(description = "权限实体")
public class SysPermission {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "权限ID", example = "1")
    private Long id;

    @Schema(description = "权限编码", example = "sys:user:add")
    private String permissionCode;

    @Schema(description = "权限名称", example = "添加用户")
    private String permissionName;

    @Schema(description = "API路径匹配规则", example = "/emp/sysUser/add")
    private String apiPath;

    @Schema(description = "权限描述")
    private String description;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
