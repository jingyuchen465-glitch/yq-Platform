package com.itcjy.emp.pojo.req;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色分页查询参数")
public class SysRolePageReq {

    @Schema(description = "当前页", example = "1")
    @Min(value = 1, message = "当前页必须大于0")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Long size = 10L;

    @Schema(description = "角色编码模糊查询", example = "LECT")
    @Size(max = 64, message = "角色编码长度不能超过64个字符")
    private String roleCode;

    @Schema(description = "角色名称模糊查询", example = "讲师")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}