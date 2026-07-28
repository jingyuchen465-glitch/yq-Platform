package com.itcjy.emp.pojo.req.exam;

import com.itcjy.common.pojo.BasePageReq;
import lombok.Data;

@Data
public class ExamRecordPageReq extends BasePageReq {
    private String studentKeyword;
    private String status;
    private String gradingStatus;
}
