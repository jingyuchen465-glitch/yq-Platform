package com.itcjy.emp.service.homework;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionGradeReq;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionItemRes;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionOverviewRes;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;

public interface IHomeworkSubmissionService extends IService<HomeworkSubmission> {

    HomeworkSubmissionOverviewRes pageByHomework(Long homeworkId, HomeworkSubmissionPageReq req);

    OssDownloadUrlRes generateDownloadUrl(Long submissionId, boolean preview);

    HomeworkSubmissionItemRes grade(Long submissionId, HomeworkSubmissionGradeReq req);
}
