package com.itcjy.emp.pojo.req.market;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "产品分页查询参数")
public class MarketProductPageReq extends BasePageReq {

    @Schema(description = "产品名称或课程名称关键字")
    @Size(max = 128, message = "搜索关键字长度不能超过128个字符")
    private String keyword;
}
