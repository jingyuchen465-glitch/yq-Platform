package com.itcjy.emp.pojo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "员工用户分页查询参数")
public class SysUserPageReq {

    @Schema(description = "当前页", example = "1")
    @Min(value = 1, message = "当前页必须大于0")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Long size = 10L;

    @Schema(description = "用户名模糊查询", example = "zhang")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    @Schema(description = "昵称模糊查询", example = "张")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Schema(description = "真实姓名模糊查询", example = "三")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    public void setReal_name(String realName) {
        if (realName != null && !realName.isBlank() && (this.realName == null || this.realName.isBlank())) {
            this.realName = realName;
        }
    }
}
