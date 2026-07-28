package com.itcjy.stu.service;

import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;
import com.itcjy.stu.pojo.DTO.StudentHomeworkSubmissionDTO;
import com.itcjy.stu.pojo.DTO.StudentHomeworkUploadUrlDTO;
import com.itcjy.stu.pojo.VO.StudentHomeworkVO;

import java.util.List;

public interface StudentHomeworkService {

    List<StudentHomeworkVO> listCurrentStudentHomeworks();

    OssUploadUrlRes generateSubmissionUploadUrl(Long homeworkId, StudentHomeworkUploadUrlDTO dto);

    OssDownloadUrlRes generateHomeworkFileUrl(Long homeworkId, boolean preview);

    void submitHomework(Long homeworkId, StudentHomeworkSubmissionDTO dto);
}
