package com.itcjy.emp.service.homework;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerUploadReq;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerVisibilityReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPrefillReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPublishReq;
import com.itcjy.emp.pojo.req.homework.HomeworkStatusPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkClassOptionRes;
import com.itcjy.emp.pojo.res.homework.HomeworkClassStatusRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPrefillRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPublishRes;
import com.itcjy.emp.pojo.res.homework.HomeworkStatusItemRes;

import java.util.List;

public interface IHomeworkService extends IService<Homework> {

    List<HomeworkClassOptionRes> listClassOptions();

    HomeworkPrefillRes prefill(HomeworkPrefillReq req);

    HomeworkPublishRes publish(HomeworkPublishReq req);

    PageResult<HomeworkClassStatusRes> pageStatus(HomeworkStatusPageReq req);

    HomeworkStatusItemRes saveAnswer(Long homeworkId, HomeworkAnswerUploadReq req);

    HomeworkStatusItemRes updateAnswerVisibility(Long homeworkId, HomeworkAnswerVisibilityReq req);
}
