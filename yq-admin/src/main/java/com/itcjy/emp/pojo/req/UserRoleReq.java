package com.itcjy.emp.pojo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户角色分配参数")
public class UserRoleReq {

    @Schema(description = "角色ID列表，传空数组表示清空用户角色", example = "[1,2]")
    @NotNull(message = "角色ID列表不能为空")
    private List<@NotNull(message = "角色ID不能为空") Long> roleIds;
}