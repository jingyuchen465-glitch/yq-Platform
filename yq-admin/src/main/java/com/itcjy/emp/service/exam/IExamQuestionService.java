package com.itcjy.emp.service.exam;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamQuestionRes;

public interface IExamQuestionService {
    PageResult<ExamQuestionRes> page(ExamQuestionPageReq req);
    ExamQuestionRes detail(Long id);
    ExamQuestionRes create(ExamQuestionSaveReq req);
    ExamQuestionRes update(Long id, ExamQuestionSaveReq req);
    ExamQuestionRes updateStatus(Long id, ExamQuestionStatusReq req);
}
