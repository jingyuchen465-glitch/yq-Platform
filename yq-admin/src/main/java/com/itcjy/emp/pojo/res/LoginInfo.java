package com.itcjy.emp.pojo.res;

import lombok.Data;

import java.util.List;

@Data
public class LoginInfo {
    /** 登录令牌（JWT Token 或 Session Token） */
    private String token;

    /** 签名密钥（用于接口签名校验） */
    private String signSecret;

    /** 用户详细信息 */
    private UserDetailRes userDetailRes;

    /** 用户角色列表 */
    private List<String> roles;

    /** 用户权限列表 */
    private List<String> permissions;
}
