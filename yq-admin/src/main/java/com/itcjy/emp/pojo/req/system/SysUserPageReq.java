package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工用户分页查询参数")
public class SysUserPageReq extends BasePageReq {

    @Schema(description = "用户名模糊查询", example = "zhang")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    @Schema(description = "昵称模糊查询", example = "张")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Schema(description = "真实姓名模糊查询", example = "三")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @Schema(description = "角色ID精确查询", example = "1")
    @Min(value = 1, message = "角色ID必须大于0")
    private Long roleId;

    public void setReal_name(String realName) {
        if (realName != null && !realName.isBlank() && (this.realName == null || this.realName.isBlank())) {
            this.realName = realName;
        }
    }
}
