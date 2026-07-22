package com.itcjy.emp.pojo.req.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色权限授权参数")
public class RolePermissionReq {

    @Schema(description = "权限ID列表，传空数组表示清空角色权限", example = "[1,2,3]")
    @NotNull(message = "权限ID列表不能为空")
    private List<@NotNull(message = "权限ID不能为空") Long> permissionIds;
}