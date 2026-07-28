package com.itcjy.emp.service.impl.exam;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.service.exam.IExamSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSubmissionServiceImpl implements IExamSubmissionService {
    private final StudentExamRecordMapper recordMapper;
    private final StudentExamAnswerMapper answerMapper;
    private final ExamPaperQuestionMapper questionMapper;
    private final ExamTimeoutOutboxMapper outboxMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamRecord submit(Long recordId, ExamSubmitReason reason) {
        StudentExamRecord record = recordMapper.selectByIdForUpdate(recordId);
        if (record == null) return null;
        if (!ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) return record;
        List<StudentExamAnswer> answers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId));
        List<Long> paperQuestionIds = answers.stream().map(StudentExamAnswer::getPaperQuestionId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ExamPaperQuestion> questions = paperQuestionIds.isEmpty() ? Map.of() :
                questionMapper.selectBatchIds(paperQuestionIds).stream()
                        .collect(Collectors.toMap(ExamPaperQuestion::getId, Function.identity()));
        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean subjective = false;
        for (StudentExamAnswer answer : answers) {
            ExamPaperQuestion question = questions.get(answer.getPaperQuestionId());
            if (question == null) {
                throw new IllegalStateException("Missing paper question snapshot: " + answer.getPaperQuestionId());
            }
            QuestionType type = QuestionType.valueOf(answer.getQuestionType());
            if (type.objective()) {
                boolean correct = Objects.equals(normalize(type, answer.getAnswerContent()),
                        normalize(type, question.getAnswerContent()));
                answer.setCorrect(correct);
                answer.setScore(correct ? answer.getQuestionScore() : BigDecimal.ZERO);
                objectiveScore = objectiveScore.add(answer.getScore());
                answerMapper.updateById(answer);
            } else {
                subjective = true;
            }
        }
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(BigDecimal.ZERO);
        record.setScore(objectiveScore);
        record.setSubmitTime(LocalDateTime.now());
        record.setSubmitReason(reason.name());
        record.setStatus(reason == ExamSubmitReason.TIMEOUT ? ExamRecordStatus.TIMEOUT.name() : ExamRecordStatus.SUBMITTED.name());
        record.setGradingStatus(subjective ? GradingStatus.PENDING.name() : GradingStatus.COMPLETED.name());
        recordMapper.updateById(record);
        outboxMapper.update(null, Wrappers.<ExamTimeoutOutbox>lambdaUpdate()
                .eq(ExamTimeoutOutbox::getRecordId, recordId)
                .set(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PROCESSED.name())
                .set(ExamTimeoutOutbox::getProcessedAt, LocalDateTime.now()));
        return record;
    }

    static String normalize(QuestionType type, String raw) {
        if (raw == null) return "";
        if (type == QuestionType.MULTIPLE) {
            return Arrays.stream(raw.toUpperCase().split(",")).map(String::trim)
                    .filter(value -> !value.isBlank()).distinct().sorted().collect(Collectors.joining(","));
        }
        return raw.trim().toUpperCase();
    }
}
