package com.itcjy.emp.service.exam;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.*;
import java.util.List;

public interface IExamManagementService {
    List<ExamAdminRes> publish(ExamPublishReq req);
    PageResult<ExamAdminRes> page(ExamPageReq req);
    ExamAdminRes detail(Long id);
    PageResult<ExamRecordRes> records(Long examId, ExamRecordPageReq req);
    ExamGradingRes gradingDetail(Long recordId);
    ExamGradingRes grade(Long recordId, ExamGradeReq req);
    ExamAdminRes updateVisibility(Long examId, ExamVisibilityReq req);
}
