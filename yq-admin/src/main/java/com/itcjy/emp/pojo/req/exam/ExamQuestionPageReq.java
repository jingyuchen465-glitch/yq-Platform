package com.itcjy.emp.pojo.req.exam;

import com.itcjy.common.pojo.BasePageReq;
import lombok.Data;

@Data
public class ExamQuestionPageReq extends BasePageReq {
    private String keyword;
    private Long courseId;
    private String stageName;
    private String questionType;
    private String difficulty;
    private String status;
}
