package com.itcjy.stu.pojo.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.interceptor.LoginSession;
import lombok.Data;

@Data
@JsonIgnoreProperties({"principalId", "principalType", "permissions"})
public class LoginInfo implements LoginSession {
    /** 登录令牌（JWT Token 或 Session Token） */
    private String token;

    /** 签名密钥（用于接口签名校验） */
    private String signSecret;

    /** 用户详细信息 */
    private StudentDetailsVO studentDetailsVO;

    @Override
    public Long getPrincipalId() {
        return studentDetailsVO == null ? null : studentDetailsVO.getId();
    }

    @Override
    public String getPrincipalType() {
        return TokenConstants.PRINCIPAL_STUDENT;
    }
}
