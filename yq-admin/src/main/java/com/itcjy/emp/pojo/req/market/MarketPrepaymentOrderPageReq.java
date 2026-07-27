package com.itcjy.emp.pojo.req.market;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预订单分页查询参数")
public class MarketPrepaymentOrderPageReq extends BasePageReq {

    @Schema(description = "学生、联系方式、产品、销售或院校关键字")
    @Size(max = 128, message = "搜索关键字长度不能超过128个字符")
    private String keyword;
}
