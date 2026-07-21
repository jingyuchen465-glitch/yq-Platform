package com.itcjy.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页请求基础参数")
public class BasePageReq {

    @Schema(description = "当前页", example = "1")
    @Min(value = 1, message = "当前页必须大于0")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Long size = 10L;
}
