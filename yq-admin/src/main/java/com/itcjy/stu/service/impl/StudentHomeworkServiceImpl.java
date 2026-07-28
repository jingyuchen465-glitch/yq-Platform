package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.homework.HomeworkMapper;
import com.itcjy.emp.mapper.homework.HomeworkSubmissionMapper;
import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import com.itcjy.emp.pojo.req.oss.OssUploadUrlReq;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;
import com.itcjy.emp.service.oss.IOssService;
import com.itcjy.stu.pojo.DTO.StudentHomeworkSubmissionDTO;
import com.itcjy.stu.pojo.DTO.StudentHomeworkUploadUrlDTO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentHomeworkVO;
import com.itcjy.stu.service.LoginService;
import com.itcjy.stu.service.StudentHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHomeworkServiceImpl implements StudentHomeworkService {

    private static final String SUBMISSION_BIZ_TYPE = "homework-submission";

    private final LoginService loginService;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final IOssService ossService;

    @Override
    public List<StudentHomeworkVO> listCurrentStudentHomeworks() {
        StudentDetailsVO student = loginService.getCurrentStudent();
        if (student.getClassId() == null) {
            return Collections.emptyList();
        }

        List<Homework> homeworks = homeworkMapper.selectList(
                Wrappers.<Homework>lambdaQuery()
                        .eq(Homework::getClassId, student.getClassId())
                        .orderByDesc(Homework::getHomeworkDate)
                        .orderByDesc(Homework::getId)
        );
        if (homeworks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, HomeworkSubmission> submissionsByHomeworkId = homeworkSubmissionMapper.selectList(
                        Wrappers.<HomeworkSubmission>lambdaQuery()
                                .eq(HomeworkSubmission::getStudentId, student.getId())
                                .eq(HomeworkSubmission::getClassId, student.getClassId())
                                .orderByDesc(HomeworkSubmission::getSubmitTime)
                                .orderByDesc(HomeworkSubmission::getId)
                ).stream()
                .collect(Collectors.toMap(
                        HomeworkSubmission::getHomeworkId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        return homeworks.stream()
                .map(homework -> StudentHomeworkVO.from(
                        homework,
                        submissionsByHomeworkId.get(homework.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OssUploadUrlRes generateSubmissionUploadUrl(Long homeworkId, StudentHomeworkUploadUrlDTO dto) {
        requireCurrentStudentHomework(homeworkId, loginService.getCurrentStudent());

        OssUploadUrlReq uploadUrlReq = new OssUploadUrlReq();
        uploadUrlReq.setFileName(dto.fileName().trim());
        uploadUrlReq.setContentType(dto.contentType().trim());
        uploadUrlReq.setBizType(SUBMISSION_BIZ_TYPE);
        return ossService.generateUploadUrl(uploadUrlReq);
    }

    @Override
    @Transactional(readOnly = true)
    public OssDownloadUrlRes generateHomeworkFileUrl(Long homeworkId, boolean preview) {
        Homework homework = requireCurrentStudentHomework(homeworkId, loginService.getCurrentStudent());
        if (homework.getContentObjectKey() == null || homework.getContentObjectKey().isBlank()) {
            throw BusinessException.DATA_ERROR.newInstance("该作业没有可查看的附件");
        }
        return ossService.generateDownloadUrl(
                homework.getContentObjectKey(), preview, homework.getContentFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitHomework(Long homeworkId, StudentHomeworkSubmissionDTO dto) {
        StudentDetailsVO student = loginService.getCurrentStudent();
        Homework homework = requireCurrentStudentHomework(homeworkId, student);
        boolean alreadySubmitted = homeworkSubmissionMapper.exists(
                Wrappers.<HomeworkSubmission>lambdaQuery()
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .eq(HomeworkSubmission::getStudentId, student.getId()));
        if (alreadySubmitted) {
            throw BusinessException.DATA_EXIST.newInstance("该作业已提交，不能重复提交");
        }

        LocalDateTime submitTime = LocalDateTime.now();
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setHomeworkId(homework.getId());
        submission.setStudentId(student.getId());
        submission.setClassId(student.getClassId());
        submission.setContentObjectKey(dto.contentObjectKey().trim());
        submission.setContentFileName(dto.contentFileName().trim());
        submission.setSubmitTime(submitTime);
        submission.setLateSubmitted(homework.getDeadline() != null && submitTime.isAfter(homework.getDeadline()));
        if (homeworkSubmissionMapper.insert(submission) != 1) {
            throw BusinessException.DATA_ERROR.newInstance("作业提交失败");
        }
    }

    private Homework requireCurrentStudentHomework(Long homeworkId, StudentDetailsVO student) {
        if (student == null || student.getId() == null || student.getClassId() == null) {
            throw BusinessException.HOMEWORK_NOT_EXIST.newInstance("作业不存在");
        }
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null || !student.getClassId().equals(homework.getClassId())) {
            throw BusinessException.HOMEWORK_NOT_EXIST.newInstance("作业不存在");
        }
        return homework;
    }
}
