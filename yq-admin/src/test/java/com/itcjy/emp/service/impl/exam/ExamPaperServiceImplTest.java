package com.itcjy.emp.service.impl.exam;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.mapper.exam.ExamPaperMapper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionMapper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionOptionMapper;
import com.itcjy.emp.mapper.exam.ExamPaperStageMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionCourseMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionMapper;
import com.itcjy.emp.mapper.exam.ExamQuestionOptionMapper;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.entity.exam.ExamPaper;
import com.itcjy.emp.pojo.entity.exam.ExamPaperQuestion;
import com.itcjy.emp.pojo.entity.exam.ExamPaperQuestionOption;
import com.itcjy.emp.pojo.entity.exam.ExamPaperStage;
import com.itcjy.emp.pojo.entity.exam.ExamQuestion;
import com.itcjy.emp.pojo.entity.exam.ExamQuestionCourse;
import com.itcjy.emp.pojo.entity.exam.ExamQuestionOption;
import com.itcjy.emp.pojo.enums.exam.PaperStatus;
import com.itcjy.emp.pojo.enums.exam.QuestionStatus;
import com.itcjy.emp.pojo.enums.exam.QuestionType;
import com.itcjy.emp.pojo.req.exam.ExamPaperSaveReq;
import com.itcjy.emp.pojo.req.exam.ExamPaperPageReq;
import com.itcjy.emp.pojo.res.exam.ExamPaperRes;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamPaperServiceImplTest {
    @Mock private ExamPaperMapper paperMapper;
    @Mock private ExamPaperStageMapper stageMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private ExamPaperQuestionOptionMapper snapshotOptionMapper;
    @Mock private ExamQuestionMapper questionMapper;
    @Mock private ExamQuestionOptionMapper questionOptionMapper;
    @Mock private ExamQuestionCourseMapper questionCourseMapper;
    @Mock private SysCourseMapper courseMapper;
    @Mock private SysCourseDetailMapper courseDetailMapper;
    private ExamPaperServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(ExamPaperStage.class);
        initTableInfo(ExamPaperQuestion.class);
        initTableInfo(ExamPaperQuestionOption.class);
        initTableInfo(ExamQuestionOption.class);
        initTableInfo(ExamQuestionCourse.class);
        service = new ExamPaperServiceImpl(paperMapper, stageMapper, paperQuestionMapper, snapshotOptionMapper,
                questionMapper, questionOptionMapper, questionCourseMapper, courseMapper, courseDetailMapper);
    }

    @Test
    @DisplayName("试卷分页筛选为空时不会触发空指针")
    @SuppressWarnings("unchecked")
    void shouldAllowNullPaperFilters() {
        when(paperMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 10));

        assertThat(service.page(new ExamPaperPageReq()).getRecords()).isEmpty();
    }

    @Test
    @DisplayName("创建试卷时固化题干、答案、解析、难度和选项快照")
    @SuppressWarnings("unchecked")
    void shouldSnapshotQuestionAndOptionsWhenCreatingPaper() {
        ExamQuestion source = sourceQuestion();
        ExamQuestionOption sourceOption = new ExamQuestionOption();
        sourceOption.setQuestionId(8L);
        sourceOption.setOptionKey("A");
        sourceOption.setOptionContent("snapshot option");
        sourceOption.setSortOrder(0);
        ExamQuestionCourse relation = new ExamQuestionCourse();
        relation.setQuestionId(8L);
        relation.setCourseId(2L);
        relation.setStageName("第一阶段");
        SysCourse course = new SysCourse();
        course.setId(2L);
        AtomicReference<ExamPaper> savedPaper = new AtomicReference<>();
        AtomicReference<ExamPaperStage> savedStage = new AtomicReference<>();
        AtomicReference<ExamPaperQuestion> savedQuestion = new AtomicReference<>();
        AtomicReference<ExamPaperQuestionOption> savedOption = new AtomicReference<>();

        when(courseMapper.selectById(2L)).thenReturn(course);
        when(courseDetailMapper.exists(any(Wrapper.class))).thenReturn(true);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(source));
        when(questionCourseMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation));
        when(questionOptionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sourceOption));
        doAnswer(invocation -> { ExamPaper paper = invocation.getArgument(0); paper.setId(5L); savedPaper.set(paper); return 1; })
                .when(paperMapper).insert(any(ExamPaper.class));
        doAnswer(invocation -> { savedStage.set(invocation.getArgument(0)); return 1; })
                .when(stageMapper).insert(any(ExamPaperStage.class));
        doAnswer(invocation -> { ExamPaperQuestion question = invocation.getArgument(0); question.setId(21L); savedQuestion.set(question); return 1; })
                .when(paperQuestionMapper).insert(any(ExamPaperQuestion.class));
        doAnswer(invocation -> { savedOption.set(invocation.getArgument(0)); return 1; })
                .when(snapshotOptionMapper).insert(any(ExamPaperQuestionOption.class));
        when(paperMapper.selectById(5L)).thenAnswer(invocation -> savedPaper.get());
        when(stageMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> List.of(savedStage.get()));
        when(paperQuestionMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> List.of(savedQuestion.get()));
        when(snapshotOptionMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> List.of(savedOption.get()));

        ExamPaperRes result = service.create(request());

        assertThat(result.totalScore()).isEqualByComparingTo("10.00");
        assertThat(result.status()).isEqualTo(PaperStatus.DRAFT.name());
        assertThat(savedQuestion.get()).satisfies(snapshot -> {
            assertThat(snapshot.getQuestionContent()).isEqualTo("original stem");
            assertThat(snapshot.getAnswerContent()).isEqualTo("A");
            assertThat(snapshot.getAnalysisContent()).isEqualTo("original analysis");
            assertThat(snapshot.getDifficulty()).isEqualTo("NORMAL");
            assertThat(snapshot.getQuestionScore()).isEqualByComparingTo("10.00");
        });
        assertThat(savedOption.get().getPaperQuestionId()).isEqualTo(21L);
        assertThat(savedOption.get().getOptionContent()).isEqualTo("snapshot option");
    }

    @Test
    @DisplayName("跨课程或阶段的题目不能加入试卷")
    @SuppressWarnings("unchecked")
    void shouldRejectQuestionOutsideSelectedCourseStages() {
        SysCourse course = new SysCourse();
        course.setId(2L);
        when(courseMapper.selectById(2L)).thenReturn(course);
        when(courseDetailMapper.exists(any(Wrapper.class))).thenReturn(true);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(sourceQuestion()));
        when(questionCourseMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("课程阶段");
        verify(paperMapper, never()).insert(any(ExamPaper.class));
    }

    private ExamPaperSaveReq request() {
        return new ExamPaperSaveReq("阶段测试", 2L, List.of("第一阶段"),
                List.of(new ExamPaperSaveReq.QuestionItem(8L, new BigDecimal("10.00"), 0)));
    }

    private ExamQuestion sourceQuestion() {
        ExamQuestion question = new ExamQuestion();
        question.setId(8L);
        question.setQuestionType(QuestionType.SINGLE.name());
        question.setQuestionContent("original stem");
        question.setAnswerContent("A");
        question.setAnalysisContent("original analysis");
        question.setDifficulty("NORMAL");
        question.setStatus(QuestionStatus.ENABLED.name());
        return question;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "exam-paper-test"), entityType);
        }
    }
}
