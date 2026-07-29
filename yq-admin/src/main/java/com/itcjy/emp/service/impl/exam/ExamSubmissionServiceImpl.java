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

/**
 * 考试交卷服务实现类
 * <p>负责学生交卷的核心逻辑：客观题自动判分、主观题标记待批改、
 * 更新考试记录状态并清理超时发件箱消息</p>
 */
@Service
@RequiredArgsConstructor
public class ExamSubmissionServiceImpl implements IExamSubmissionService {
    private final StudentExamRecordMapper recordMapper;
    private final StudentExamAnswerMapper answerMapper;
    private final ExamPaperQuestionMapper questionMapper;
    private final ExamTimeoutOutboxMapper outboxMapper;

    /**
     * 提交答卷
     * <p>使用悲观锁防止并发交卷，自动判定客观题得分，
     * 根据是否含主观题决定批改状态，并标记超时发件箱为已处理</p>
     *
     * @param recordId 考试记录ID
     * @param reason   交卷原因（手动/超时/缺考）
     * @return 更新后的考试记录，若记录不存在或已提交则直接返回
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamRecord submit(Long recordId, ExamSubmitReason reason) {
        // 悲观锁查询，防止并发交卷
        StudentExamRecord record = recordMapper.selectByIdForUpdate(recordId);
        if (record == null) return null;
        // 只有进行中的考试才能交卷
        if (!ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) return record;
        // 查询学生的所有答题记录
        List<StudentExamAnswer> answers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId));
        // 获取对应的试卷题目快照
        List<Long> paperQuestionIds = answers.stream().map(StudentExamAnswer::getPaperQuestionId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ExamPaperQuestion> questions = paperQuestionIds.isEmpty() ? Map.of() :
                questionMapper.selectBatchIds(paperQuestionIds).stream()
                        .collect(Collectors.toMap(ExamPaperQuestion::getId, Function.identity()));
        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean subjective = false;
        // 逐题判定：客观题自动判分，主观题标记待批改
        for (StudentExamAnswer answer : answers) {
            ExamPaperQuestion question = questions.get(answer.getPaperQuestionId());
            if (question == null) {
                throw new IllegalStateException("Missing paper question snapshot: " + answer.getPaperQuestionId());
            }
            QuestionType type = QuestionType.valueOf(answer.getQuestionType());
            if (type.objective()) {
                // 客观题：标准化后对比学生答案与标准答案
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
        // 更新考试记录：设置成绩、交卷时间、状态和批改状态
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(BigDecimal.ZERO);
        record.setScore(objectiveScore);
        record.setSubmitTime(LocalDateTime.now());
        record.setSubmitReason(reason.name());
        record.setStatus(reason == ExamSubmitReason.TIMEOUT ? ExamRecordStatus.TIMEOUT.name() : ExamRecordStatus.SUBMITTED.name());
        record.setGradingStatus(subjective ? GradingStatus.PENDING.name() : GradingStatus.COMPLETED.name());
        recordMapper.updateById(record);
        // 将超时发件箱消息标记为已处理
        outboxMapper.update(null, Wrappers.<ExamTimeoutOutbox>lambdaUpdate()
                .eq(ExamTimeoutOutbox::getRecordId, recordId)
                .set(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PROCESSED.name())
                .set(ExamTimeoutOutbox::getProcessedAt, LocalDateTime.now()));
        return record;
    }

    /**
     * 标准化答案格式（用于客观题判分对比）
     * <p>多选题去重排序后以逗号拼接，其他题型去空格转大写</p>
     *
     * @param type 题型
     * @param raw  原始答案
     * @return 标准化后的答案
     */
    static String normalize(QuestionType type, String raw) {
        if (raw == null) return "";
        if (type == QuestionType.MULTIPLE) {
            return Arrays.stream(raw.toUpperCase().split(",")).map(String::trim)
                    .filter(value -> !value.isBlank()).distinct().sorted().collect(Collectors.joining(","));
        }
        return raw.trim().toUpperCase();
    }
}
