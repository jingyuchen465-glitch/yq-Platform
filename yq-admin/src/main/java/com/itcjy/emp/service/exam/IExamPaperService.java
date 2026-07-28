package com.itcjy.emp.service.exam;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamPaperRes;

public interface IExamPaperService {
    PageResult<ExamPaperRes> page(ExamPaperPageReq req);
    ExamPaperRes detail(Long id);
    ExamPaperRes create(ExamPaperSaveReq req);
    ExamPaperRes update(Long id, ExamPaperSaveReq req);
}
