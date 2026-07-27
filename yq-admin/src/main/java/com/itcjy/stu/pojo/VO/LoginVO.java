package com.itcjy.stu.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生登录响应")
public class LoginVO {

    @Schema(description = "JWT token，后续请求放在 Authorization 请求头中")
    private String token;

    @Schema(description = "请求签名密钥，前端用它生成 X-Sign")
    private String signSecret;

    @Schema(description = "当前登录学生详情")
    private StudentDetailsVO studentDetailsVO;

    public static LoginVO of(String token, String signSecret, StudentDetailsVO studentDetailsVO) {
        LoginVO response = new LoginVO();
        response.setToken(token);
        response.setSignSecret(signSecret);
        response.setStudentDetailsVO(studentDetailsVO);
        return response;
    }
}
