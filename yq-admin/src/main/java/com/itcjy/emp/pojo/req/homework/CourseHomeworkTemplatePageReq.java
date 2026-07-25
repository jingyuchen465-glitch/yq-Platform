package com.itcjy.emp.pojo.req.homework;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.BasePageReq;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourseHomeworkTemplatePageReq extends BasePageReq {

    @Positive(message = "课程ID必须大于0")
    private Long courseId;

    @Size(max = 128, message = "课程名称不能超过128个字符")
    private String courseName;

    @StatusEnum(enumClass = ActiveEnum.class, message = "状态只能是ACTIVE或INACTIVE")
    private String status;
}
