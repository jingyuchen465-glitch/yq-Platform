package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.exam.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StudentExamVO(
        Long examId, Long recordId, String paperName, BigDecimal totalScore,
        String recordStatus, LocalDateTime serverTime, LocalDateTime deadlineTime,
        List<QuestionItem> questions) {

    public static StudentExamVO from(Exam exam, ExamPaper paper, StudentExamRecord record,
                                     List<ExamPaperQuestion> questions,
                                     Map<Long, List<ExamPaperQuestionOption>> options,
                                     Map<Long, StudentExamAnswer> answers) {
        return new StudentExamVO(exam.getId(), record.getId(), paper.getPaperName(), paper.getTotalScore(),
                record.getStatus(), LocalDateTime.now(), record.getDeadlineTime(),
                questions.stream().map(question -> QuestionItem.from(question,
                        options.getOrDefault(question.getId(), List.of()), answers.get(question.getId()))).toList());
    }

    public record QuestionItem(
            Long paperQuestionId, String questionType, String questionContent,
            BigDecimal questionScore, Integer sortOrder, String savedAnswer,
            List<OptionItem> options) {
        static QuestionItem from(ExamPaperQuestion question,
                                 List<ExamPaperQuestionOption> options,
                                 StudentExamAnswer answer) {
            return new QuestionItem(question.getId(), question.getQuestionType(), question.getQuestionContent(),
                    question.getQuestionScore(), question.getSortOrder(),
                    answer == null ? null : answer.getAnswerContent(),
                    options.stream().map(OptionItem::from).toList());
        }
    }

    public record OptionItem(String optionKey, String optionContent, Integer sortOrder) {
        static OptionItem from(ExamPaperQuestionOption option) {
            return new OptionItem(option.getOptionKey(), option.getOptionContent(), option.getSortOrder());
        }
    }
}
