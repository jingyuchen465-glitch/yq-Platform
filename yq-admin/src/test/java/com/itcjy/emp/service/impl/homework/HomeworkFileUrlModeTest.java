package com.itcjy.emp.service.impl.homework;

import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.mapper.homework.CourseHomeworkTemplateMapper;
import com.itcjy.emp.mapper.homework.HomeworkMapper;
import com.itcjy.emp.mapper.homework.HomeworkSubmissionMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.pojo.entity.CourseHomeworkTemplate;
import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateDownloadRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePreviewRes;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.service.oss.IOssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkFileUrlModeTest {

    @Mock
    private HomeworkSubmissionMapper submissionMapper;
    @Mock
    private HomeworkMapper homeworkMapper;
    @Mock
    private SysClassMapper classMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private CourseHomeworkTemplateMapper templateMapper;
    @Mock
    private SysCourseMapper courseMapper;
    @Mock
    private SysCourseDetailMapper courseDetailMapper;
    @Mock
    private IOssService ossService;

    private HomeworkSubmissionServiceImpl submissionService;
    private CourseHomeworkTemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        submissionService = new HomeworkSubmissionServiceImpl(
                homeworkMapper, classMapper, userMapper, ossService);
        ReflectionTestUtils.setField(submissionService, "baseMapper", submissionMapper);
        templateService = new CourseHomeworkTemplateServiceImpl(
                templateMapper, courseMapper, courseDetailMapper, ossService);
    }

    @Test
    @DisplayName("学生提交预览时向 OSS 传入 true")
    void shouldRequestPreviewUrlForSubmission() {
        HomeworkSubmission submission = submission(11L, "homework/submission.md", "学生作业.md");
        OssDownloadUrlRes signed = new OssDownloadUrlRes("https://oss.example/preview", 3600);
        when(submissionMapper.selectById(11L)).thenReturn(submission);
        when(ossService.generateDownloadUrl(
                "homework/submission.md", true, "学生作业.md")).thenReturn(signed);

        OssDownloadUrlRes result = submissionService.generateDownloadUrl(11L, true);

        assertThat(result).isSameAs(signed);
        verify(ossService).generateDownloadUrl(
                "homework/submission.md", true, "学生作业.md");
    }

    @Test
    @DisplayName("学生提交下载时向 OSS 传入 false")
    void shouldRequestAttachmentUrlForSubmission() {
        HomeworkSubmission submission = submission(12L, "homework/submission.pdf", "学生作业.pdf");
        OssDownloadUrlRes signed = new OssDownloadUrlRes("https://oss.example/download", 3600);
        when(submissionMapper.selectById(12L)).thenReturn(submission);
        when(ossService.generateDownloadUrl(
                "homework/submission.pdf", false, "学生作业.pdf")).thenReturn(signed);

        OssDownloadUrlRes result = submissionService.generateDownloadUrl(12L, false);

        assertThat(result).isSameAs(signed);
        verify(ossService).generateDownloadUrl(
                "homework/submission.pdf", false, "学生作业.pdf");
    }

    @Test
    @DisplayName("课程作业标准预览时向 OSS 传入 true")
    void shouldRequestPreviewUrlForCourseTemplate() {
        CourseHomeworkTemplate template = template(21L, "template/standard.pdf", "Java作业标准.pdf");
        when(templateMapper.selectById(21L)).thenReturn(template);
        when(ossService.generateDownloadUrl(
                "template/standard.pdf", true, "Java作业标准.pdf"))
                .thenReturn(new OssDownloadUrlRes("https://oss.example/preview", 3600));

        CourseHomeworkTemplatePreviewRes result = templateService.getPreview(21L);

        assertThat(result.previewUrl()).isEqualTo("https://oss.example/preview");
        verify(ossService).generateDownloadUrl(
                "template/standard.pdf", true, "Java作业标准.pdf");
    }

    @Test
    @DisplayName("课程作业标准下载时向 OSS 传入 false")
    void shouldRequestAttachmentUrlForCourseTemplate() {
        CourseHomeworkTemplate template = template(22L, "template/standard.docx", "Java作业标准.docx");
        when(templateMapper.selectById(22L)).thenReturn(template);
        when(ossService.generateDownloadUrl(
                "template/standard.docx", false, "Java作业标准.docx"))
                .thenReturn(new OssDownloadUrlRes("https://oss.example/download", 3600));

        CourseHomeworkTemplateDownloadRes result = templateService.getDownload(22L);

        assertThat(result.downloadUrl()).isEqualTo("https://oss.example/download");
        verify(ossService).generateDownloadUrl(
                "template/standard.docx", false, "Java作业标准.docx");
    }

    private HomeworkSubmission submission(Long id, String objectKey, String fileName) {
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(id);
        submission.setContentObjectKey(objectKey);
        submission.setContentFileName(fileName);
        return submission;
    }

    private CourseHomeworkTemplate template(Long id, String objectKey, String fileName) {
        CourseHomeworkTemplate template = new CourseHomeworkTemplate();
        template.setId(id);
        template.setContentObjectKey(objectKey);
        template.setContentFileName(fileName);
        return template;
    }
}
