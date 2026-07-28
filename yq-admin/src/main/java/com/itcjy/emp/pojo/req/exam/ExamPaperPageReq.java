package com.itcjy.emp.pojo.req.exam;

import com.itcjy.common.pojo.BasePageReq;
import lombok.Data;

@Data
public class ExamPaperPageReq extends BasePageReq {
    private String keyword;
    private Long courseId;
    private String status;
}
