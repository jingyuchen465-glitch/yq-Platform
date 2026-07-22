package com.itcjy.emp.pojo.req;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "校区分页查询参数")
public class SysCampusPageReq extends BasePageReq {

    @Schema(description = "校区地点模糊查询", example = "北京")
    @Size(max = 255, message = "校区地点长度不能超过255个字符")
    private String campusLocation;

    @Schema(description = "负责人模糊查询", example = "张")
    @Size(max = 64, message = "负责人长度不能超过64个字符")
    private String managerName;
}
