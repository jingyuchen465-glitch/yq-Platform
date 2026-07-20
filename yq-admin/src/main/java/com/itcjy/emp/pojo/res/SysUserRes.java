package com.itcjy.emp.pojo.res;

import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户响应视图对象，携带角色列表")
public class SysUserRes extends SysUser {

    @Schema(description = "用户拥有的角色列表")
    private List<SysRole> roles;
}
