package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StudentHomeworkServiceImplTest {

    private LoginService loginService;
    private HomeworkMapper homeworkMapper;
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    private IOssService ossService;
    private StudentHomeworkServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(Homework.class);
        initTableInfo(HomeworkSubmission.class);
        loginService = mock(LoginService.class);
        homeworkMapper = mock(HomeworkMapper.class);
        homeworkSubmissionMapper = mock(HomeworkSubmissionMapper.class);
        ossService = mock(IOssService.class);
        service = new StudentHomeworkServiceImpl(loginService, homeworkMapper, homeworkSubmissionMapper, ossService);
    }

    @Test
    @DisplayName("未分班学生不查询作业数据并返回空列表")
    void shouldReturnEmptyListWhenStudentHasNoClass() {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        when(loginService.getCurrentStudent()).thenReturn(student);

        List<StudentHomeworkVO> result = service.listCurrentStudentHomeworks();

        assertThat(result).isEmpty();
        verifyNoInteractions(homeworkMapper, homeworkSubmissionMapper);
    }

    @Test
    @DisplayName("已分班学生仅获得班级作业及自己的提交状态")
    @SuppressWarnings("unchecked")
    void shouldMergeOnlyCurrentStudentSubmissions() {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        student.setClassId(42L);
        Homework homework = new Homework();
        homework.setId(100L);
        homework.setTitle("集合练习");
        homework.setHomeworkDate(LocalDate.of(2026, 7, 27));
        homework.setDeadline(LocalDateTime.of(2026, 7, 28, 18, 0));
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(200L);
        submission.setHomeworkId(100L);
        submission.setStudentId(9L);
        submission.setClassId(42L);
        submission.setContentFileName("answer.md");
        submission.setSubmitTime(LocalDateTime.of(2026, 7, 27, 20, 0));
        submission.setScore(95);

        when(loginService.getCurrentStudent()).thenReturn(student);
        when(homeworkMapper.selectList(any(Wrapper.class))).thenReturn(List.of(homework));
        when(homeworkSubmissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(submission));

        List<StudentHomeworkVO> result = service.listCurrentStudentHomeworks();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(100L);
            assertThat(item.title()).isEqualTo("集合练习");
            assertThat(item.submissionId()).isEqualTo(200L);
            assertThat(item.submissionFileName()).isEqualTo("answer.md");
            assertThat(item.score()).isEqualTo(95);
        });
    }

    @Test
    @DisplayName("学生查看本班作业附件时透传预览模式")
    void shouldGeneratePreviewUrlForCurrentClassHomework() {
        StudentDetailsVO student = student(9L, 42L);
        Homework homework = homework(100L, 42L);
        homework.setContentObjectKey("homework/2026/07/28/assignment.md");
        homework.setContentFileName("assignment.md");
        OssDownloadUrlRes signed = new OssDownloadUrlRes("https://oss.example/preview", 3600);
        when(loginService.getCurrentStudent()).thenReturn(student);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        when(ossService.generateDownloadUrl(homework.getContentObjectKey(), true, homework.getContentFileName()))
                .thenReturn(signed);

        assertThat(service.generateHomeworkFileUrl(100L, true)).isSameAs(signed);

        verify(ossService).generateDownloadUrl(homework.getContentObjectKey(), true, homework.getContentFileName());
    }

    @Test
    @DisplayName("学生上传作业时使用固定的 OSS 提交目录")
    void shouldUseSubmissionDirectoryForUploadUrl() {
        StudentDetailsVO student = student(9L, 42L);
        Homework homework = homework(100L, 42L);
        OssUploadUrlRes signed = new OssUploadUrlRes("https://oss.example/upload", "homework-submission/2026/07/28/a.md", 300);
        when(loginService.getCurrentStudent()).thenReturn(student);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        when(ossService.generateUploadUrl(any(OssUploadUrlReq.class))).thenReturn(signed);

        assertThat(service.generateSubmissionUploadUrl(100L,
                new StudentHomeworkUploadUrlDTO("answer.md", "text/markdown"))).isSameAs(signed);

        ArgumentCaptor<OssUploadUrlReq> request = ArgumentCaptor.forClass(OssUploadUrlReq.class);
        verify(ossService).generateUploadUrl(request.capture());
        assertThat(request.getValue().getBizType()).isEqualTo("homework-submission");
        assertThat(request.getValue().getFileName()).isEqualTo("answer.md");
    }

    @Test
    @DisplayName("学生提交后保存 OSS 对象信息并标记逾期状态")
    @SuppressWarnings("unchecked")
    void shouldSaveSubmissionForCurrentStudent() {
        StudentDetailsVO student = student(9L, 42L);
        Homework homework = homework(100L, 42L);
        homework.setDeadline(LocalDateTime.now().minusMinutes(1));
        when(loginService.getCurrentStudent()).thenReturn(student);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        when(homeworkSubmissionMapper.exists(any(Wrapper.class))).thenReturn(false);
        when(homeworkSubmissionMapper.insert(any(HomeworkSubmission.class))).thenReturn(1);

        service.submitHomework(100L, new StudentHomeworkSubmissionDTO(
                "homework-submission/2026/07/28/answer.md", "answer.md"));

        ArgumentCaptor<HomeworkSubmission> submission = ArgumentCaptor.forClass(HomeworkSubmission.class);
        verify(homeworkSubmissionMapper).insert(submission.capture());
        assertThat(submission.getValue().getHomeworkId()).isEqualTo(100L);
        assertThat(submission.getValue().getStudentId()).isEqualTo(9L);
        assertThat(submission.getValue().getClassId()).isEqualTo(42L);
        assertThat(submission.getValue().getLateSubmitted()).isTrue();
    }

    private StudentDetailsVO student(Long studentId, Long classId) {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(studentId);
        student.setClassId(classId);
        return student;
    }

    private Homework homework(Long homeworkId, Long classId) {
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setClassId(classId);
        return homework;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "student-homework-test"),
                    entityType
            );
        }
    }
}
