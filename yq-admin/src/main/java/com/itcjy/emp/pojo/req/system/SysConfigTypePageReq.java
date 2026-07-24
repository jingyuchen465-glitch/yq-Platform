package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigTypePageReq extends BasePageReq {
    @Size(max = 64, message = "配置类型编码不能超过64个字符")
    private String typeCode;
    @Size(max = 64, message = "配置类型名称不能超过64个字符")
    private String typeName;
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}
