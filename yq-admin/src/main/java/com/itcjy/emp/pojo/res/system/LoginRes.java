package com.itcjy.emp.pojo.res.system;

import lombok.Data;

/**
 * 登录响应对象。
 * <p>
 * 登录成功后返回 JWT、签名密钥、用户信息、角色和权限。
 */
@Data
public class LoginRes {

    /** JWT token，后续请求放在 Authorization 请求头中 */
    private String token;

    /** 请求签名密钥，前端用它生成 X-Sign */
    private String signSecret;

    /** 当前登录用户详情 */
    private UserDetailRes userDetailRes;

}
