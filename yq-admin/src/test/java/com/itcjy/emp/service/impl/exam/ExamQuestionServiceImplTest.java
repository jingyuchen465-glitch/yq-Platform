package com.itcjy.emp.service.impl.exam;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionCourseMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionOptionMapper;
import com.itcjy.emp.pojo.entity.exam.ExamQuestion;
import com.itcjy.emp.pojo.req.exam.ExamQuestionPageReq;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamQuestionServiceImplTest {
    @Mock private ExamQuestionMapper questionMapper;
    @Mock private ExamQuestionOptionMapper optionMapper;
    @Mock private ExamQuestionCourseMapper relationMapper;
    @Mock private SysCourseMapper courseMapper;
    @Mock private SysCourseDetailMapper courseDetailMapper;
    private ExamQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(ExamQuestion.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "exam-question-test"), ExamQuestion.class);
        }
        service = new ExamQuestionServiceImpl(
                questionMapper, optionMapper, relationMapper, courseMapper, courseDetailMapper);
    }

    @Test
    @DisplayName("题库分页筛选为空或空白时不会触发空指针")
    @SuppressWarnings("unchecked")
    void shouldAllowNullAndBlankQuestionFilters() {
        when(questionMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 10));
        ExamQuestionPageReq request = new ExamQuestionPageReq();
        request.setQuestionType(null);
        request.setDifficulty("  ");
        request.setStatus(null);

        assertThat(service.page(request).getRecords()).isEmpty();
    }
}
