package com.itcjy.emp.pojo.res.exam;

import com.itcjy.emp.pojo.entity.exam.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ExamPaperRes(
        Long id,
        String paperName,
        Long courseId,
        BigDecimal totalScore,
        String status,
        Long createdBy,
        List<String> stageNames,
        List<QuestionItem> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ExamPaperRes from(ExamPaper paper,
                                    List<ExamPaperStage> stages,
                                    List<ExamPaperQuestion> questions,
                                    Map<Long, List<ExamPaperQuestionOption>> options) {
        return new ExamPaperRes(paper.getId(), paper.getPaperName(), paper.getCourseId(),
                paper.getTotalScore(), paper.getStatus(), paper.getCreatedBy(),
                stages.stream().map(ExamPaperStage::getStageName).toList(),
                questions.stream().map(item -> QuestionItem.from(
                        item, options.getOrDefault(item.getId(), List.of()))).toList(),
                paper.getCreatedAt(), paper.getUpdatedAt());
    }

    public record QuestionItem(
            Long id, Long questionId, String questionType, String questionContent,
            String answerContent, String analysisContent, String difficulty,
            BigDecimal questionScore, Integer sortOrder, List<OptionItem> options) {
        static QuestionItem from(ExamPaperQuestion question, List<ExamPaperQuestionOption> options) {
            return new QuestionItem(question.getId(), question.getQuestionId(), question.getQuestionType(),
                    question.getQuestionContent(), question.getAnswerContent(), question.getAnalysisContent(),
                    question.getDifficulty(), question.getQuestionScore(), question.getSortOrder(),
                    options.stream().map(OptionItem::from).toList());
        }
    }

    public record OptionItem(Long id, String optionKey, String optionContent, Integer sortOrder) {
        static OptionItem from(ExamPaperQuestionOption option) {
            return new OptionItem(option.getId(), option.getOptionKey(), option.getOptionContent(), option.getSortOrder());
        }
    }
}
