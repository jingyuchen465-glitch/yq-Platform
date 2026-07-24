package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigItemPageReq extends BasePageReq {
    private Long typeId;
    @Size(max = 64, message = "配置项键不能超过64个字符")
    private String itemKey;
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}
