package com.itcjy.emp.service.impl.exam;

import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.exam.ExamMapper;
import com.itcjy.emp.mapper.exam.ExamPaperMapper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionMapper;
import com.itcjy.emp.mapper.exam.StudentExamAnswerMapper;
import com.itcjy.emp.mapper.exam.StudentExamRecordMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.pojo.entity.exam.Exam;
import com.itcjy.emp.pojo.req.exam.ExamVisibilityReq;
import com.itcjy.stu.mapper.StudentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamManagementServiceImplTest {

    @Test
    @DisplayName("整场考试关闭前不能公布答卷结果")
    void shouldRejectVisibilityChangeBeforeCloseTime() {
        ExamMapper examMapper = mock(ExamMapper.class);
        Exam exam = new Exam();
        exam.setId(7L);
        exam.setCloseTime(LocalDateTime.now().plusMinutes(1));
        when(examMapper.selectById(7L)).thenReturn(exam);
        ExamManagementServiceImpl service = new ExamManagementServiceImpl(
                examMapper, mock(ExamPaperMapper.class), mock(StudentExamRecordMapper.class),
                mock(StudentExamAnswerMapper.class), mock(ExamPaperQuestionMapper.class),
                mock(SysClassMapper.class), mock(SysUserMapper.class), mock(StudentMapper.class));

        assertThatThrownBy(() -> service.updateVisibility(7L, new ExamVisibilityReq(true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关闭后");
        verify(examMapper, never()).updateById(exam);
    }
}
