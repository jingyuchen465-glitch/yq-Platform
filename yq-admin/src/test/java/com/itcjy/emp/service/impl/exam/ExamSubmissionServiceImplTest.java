package com.itcjy.emp.service.impl.exam;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.emp.mapper.exam.ExamPaperQuestionMapper;
import com.itcjy.emp.mapper.exam.ExamTimeoutOutboxMapper;
import com.itcjy.emp.mapper.exam.StudentExamAnswerMapper;
import com.itcjy.emp.mapper.exam.StudentExamRecordMapper;
import com.itcjy.emp.pojo.entity.exam.ExamPaperQuestion;
import com.itcjy.emp.pojo.entity.exam.ExamTimeoutOutbox;
import com.itcjy.emp.pojo.entity.exam.StudentExamAnswer;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import com.itcjy.emp.pojo.enums.exam.ExamRecordStatus;
import com.itcjy.emp.pojo.enums.exam.ExamSubmitReason;
import com.itcjy.emp.pojo.enums.exam.GradingStatus;
import com.itcjy.emp.pojo.enums.exam.QuestionType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamSubmissionServiceImplTest {
    @Mock private StudentExamRecordMapper recordMapper;
    @Mock private StudentExamAnswerMapper answerMapper;
    @Mock private ExamPaperQuestionMapper questionMapper;
    @Mock private ExamTimeoutOutboxMapper outboxMapper;
    private ExamSubmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(StudentExamAnswer.class);
        initTableInfo(ExamTimeoutOutbox.class);
        service = new ExamSubmissionServiceImpl(recordMapper, answerMapper, questionMapper, outboxMapper);
    }

    @Test
    @DisplayName("多选答案顺序和重复项不影响客观题判分")
    @SuppressWarnings("unchecked")
    void shouldGradeMultipleChoiceIndependentOfOrder() {
        StudentExamRecord record = inProgressRecord();
        StudentExamAnswer answer = answer(21L, QuestionType.MULTIPLE, "C,A,A", "5.00");
        ExamPaperQuestion question = question(21L, QuestionType.MULTIPLE, "A,C");
        when(recordMapper.selectByIdForUpdate(10L)).thenReturn(record);
        when(answerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(answer));
        when(questionMapper.selectBatchIds(List.of(21L))).thenReturn(List.of(question));

        StudentExamRecord submitted = service.submit(10L, ExamSubmitReason.MANUAL);

        assertThat(answer.getCorrect()).isTrue();
        assertThat(answer.getScore()).isEqualByComparingTo("5.00");
        assertThat(submitted.getObjectiveScore()).isEqualByComparingTo("5.00");
        assertThat(submitted.getScore()).isEqualByComparingTo("5.00");
        assertThat(submitted.getStatus()).isEqualTo(ExamRecordStatus.SUBMITTED.name());
        assertThat(submitted.getGradingStatus()).isEqualTo(GradingStatus.COMPLETED.name());
        verify(answerMapper).updateById(answer);
        verify(recordMapper).updateById(record);
    }

    @Test
    @DisplayName("含主观题的答卷提交后进入待批改状态")
    @SuppressWarnings("unchecked")
    void shouldMarkSubmissionPendingWhenSubjectiveQuestionExists() {
        StudentExamRecord record = inProgressRecord();
        StudentExamAnswer objective = answer(21L, QuestionType.SINGLE, "A", "3.00");
        StudentExamAnswer subjective = answer(22L, QuestionType.SHORT, "student response", "7.00");
        when(recordMapper.selectByIdForUpdate(10L)).thenReturn(record);
        when(answerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(objective, subjective));
        when(questionMapper.selectBatchIds(List.of(21L, 22L))).thenReturn(List.of(
                question(21L, QuestionType.SINGLE, "A"),
                question(22L, QuestionType.SHORT, "reference")));

        StudentExamRecord submitted = service.submit(10L, ExamSubmitReason.TIMEOUT);

        assertThat(submitted.getStatus()).isEqualTo(ExamRecordStatus.TIMEOUT.name());
        assertThat(submitted.getGradingStatus()).isEqualTo(GradingStatus.PENDING.name());
        assertThat(submitted.getObjectiveScore()).isEqualByComparingTo("3.00");
        assertThat(subjective.getScore()).isNull();
    }

    @Test
    @DisplayName("重复提交已经封卷的记录时保持幂等")
    void shouldBeIdempotentForAlreadySubmittedRecord() {
        StudentExamRecord record = inProgressRecord();
        record.setStatus(ExamRecordStatus.SUBMITTED.name());
        when(recordMapper.selectByIdForUpdate(10L)).thenReturn(record);

        assertThat(service.submit(10L, ExamSubmitReason.TIMEOUT)).isSameAs(record);

        verify(answerMapper, never()).selectList(any());
        verify(recordMapper, never()).updateById(any(StudentExamRecord.class));
        verify(outboxMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("答案标准化统一大小写、去重并排序")
    void shouldNormalizeObjectiveAnswers() {
        assertThat(ExamSubmissionServiceImpl.normalize(QuestionType.MULTIPLE, " c, A, c ")).isEqualTo("A,C");
        assertThat(ExamSubmissionServiceImpl.normalize(QuestionType.JUDGE, " true ")).isEqualTo("TRUE");
        assertThat(ExamSubmissionServiceImpl.normalize(QuestionType.SINGLE, null)).isEmpty();
    }

    private StudentExamRecord inProgressRecord() {
        StudentExamRecord record = new StudentExamRecord();
        record.setId(10L);
        record.setStatus(ExamRecordStatus.IN_PROGRESS.name());
        return record;
    }

    private StudentExamAnswer answer(Long paperQuestionId, QuestionType type, String content, String score) {
        StudentExamAnswer answer = new StudentExamAnswer();
        answer.setId(paperQuestionId + 100);
        answer.setRecordId(10L);
        answer.setPaperQuestionId(paperQuestionId);
        answer.setQuestionType(type.name());
        answer.setQuestionScore(new BigDecimal(score));
        answer.setAnswerContent(content);
        return answer;
    }

    private ExamPaperQuestion question(Long id, QuestionType type, String answer) {
        ExamPaperQuestion question = new ExamPaperQuestion();
        question.setId(id);
        question.setQuestionType(type.name());
        question.setAnswerContent(answer);
        return question;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "exam-submission-test"), entityType);
        }
    }
}
