package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "校区新增请求参数")
public class SysCampusReq {

    @Schema(description = "校区地点", example = "北京市朝阳区")
    @NotBlank(message = "校区地点不能为空")
    @Size(max = 255, message = "校区地点长度不能超过255个字符")
    private String campusLocation;

    @Schema(description = "负责人", example = "张三")
    @NotBlank(message = "负责人不能为空")
    @Size(max = 64, message = "负责人长度不能超过64个字符")
    private String managerName;

    @Schema(description = "负责人电话", example = "13800138000")
    @NotBlank(message = "负责人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String managerPhone;
}
