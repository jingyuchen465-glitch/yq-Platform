package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.exam.ExamMapper;
import com.itcjy.emp.mapper.exam.ExamPaperMapper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionMapper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionOptionMapper;
import com.itcjy.emp.mapper.exam.StudentExamAnswerMapper;
import com.itcjy.emp.mapper.exam.StudentExamRecordMapper;
import com.itcjy.emp.pojo.entity.exam.Exam;
import com.itcjy.emp.pojo.entity.exam.ExamPaper;
import com.itcjy.emp.pojo.entity.exam.StudentExamAnswer;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import com.itcjy.emp.pojo.enums.exam.ExamRecordStatus;
import com.itcjy.emp.service.exam.IExamSubmissionService;
import com.itcjy.emp.service.exam.IExamTimeoutService;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.service.LoginService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {
    @Mock private LoginService loginService;
    @Mock private ExamMapper examMapper;
    @Mock private ExamPaperMapper paperMapper;
    @Mock private ExamPaperQuestionMapper questionMapper;
    @Mock private ExamPaperQuestionOptionMapper optionMapper;
    @Mock private StudentExamRecordMapper recordMapper;
    @Mock private StudentExamAnswerMapper answerMapper;
    @Mock private IExamTimeoutService timeoutService;
    @Mock private IExamSubmissionService submissionService;
    private StudentExamServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(StudentExamRecord.class);
        initTableInfo(StudentExamAnswer.class);
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        when(loginService.getCurrentStudent()).thenReturn(student);
        service = new StudentExamServiceImpl(loginService, examMapper, paperMapper, questionMapper,
                optionMapper, recordMapper, answerMapper, timeoutService, submissionService);
    }

    @Test
    @DisplayName("开始考试时以服务端时间计算个人完整答题时长")
    @SuppressWarnings("unchecked")
    void shouldCalculatePersonalDeadlineWhenStarting() {
        LocalDateTime before = LocalDateTime.now();
        Exam exam = exam(30);
        StudentExamRecord notStarted = record(9L, ExamRecordStatus.NOT_STARTED);
        StudentExamRecord inProgress = record(9L, ExamRecordStatus.IN_PROGRESS);
        inProgress.setDeadlineTime(before.plusMinutes(30));
        ExamPaper paper = new ExamPaper();
        paper.setId(3L);
        paper.setPaperName("Java 阶段测试");
        paper.setTotalScore(new BigDecimal("100"));
        when(examMapper.selectById(1L)).thenReturn(exam);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(notStarted);
        when(recordMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(recordMapper.selectById(11L)).thenReturn(inProgress);
        when(questionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(answerMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(paperMapper.selectById(3L)).thenReturn(paper);

        service.start(1L);

        ArgumentCaptor<LocalDateTime> deadline = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(timeoutService).schedule(org.mockito.ArgumentMatchers.eq(11L), deadline.capture());
        assertThat(deadline.getValue()).isAfterOrEqualTo(before.plusMinutes(30));
        assertThat(deadline.getValue()).isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(30));
    }

    @Test
    @DisplayName("超过最晚入场时间的学生不能开始考试")
    @SuppressWarnings("unchecked")
    void shouldRejectStartAfterEntryDeadline() {
        Exam exam = exam(30);
        exam.setEntryDeadlineTime(LocalDateTime.now().minusSeconds(1));
        when(examMapper.selectById(1L)).thenReturn(exam);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record(9L, ExamRecordStatus.NOT_STARTED));

        assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最晚入场");
        verify(timeoutService, never()).schedule(any(), any());
    }

    @Test
    @DisplayName("学生不能读取其他学生的考试记录")
    void shouldRejectForeignRecordAccess() {
        when(recordMapper.selectById(11L)).thenReturn(record(10L, ExamRecordStatus.IN_PROGRESS));

        assertThatThrownBy(() -> service.detail(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("记录不存在");
        verify(examMapper, never()).selectById(any());
    }

    private Exam exam(int durationMinutes) {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setPaperId(3L);
        exam.setStartTime(LocalDateTime.now().minusMinutes(1));
        exam.setEntryDeadlineTime(LocalDateTime.now().plusMinutes(10));
        exam.setDurationMinutes(durationMinutes);
        exam.setCloseTime(exam.getEntryDeadlineTime().plusMinutes(durationMinutes));
        return exam;
    }

    private StudentExamRecord record(Long studentId, ExamRecordStatus status) {
        StudentExamRecord record = new StudentExamRecord();
        record.setId(11L);
        record.setExamId(1L);
        record.setStudentId(studentId);
        record.setStatus(status.name());
        return record;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "student-exam-test"), entityType);
        }
    }
}
