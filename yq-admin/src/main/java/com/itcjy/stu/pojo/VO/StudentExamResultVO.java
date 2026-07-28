package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.exam.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StudentExamResultVO(
        Long examId, Long recordId, String paperName, BigDecimal totalScore,
        BigDecimal score, String status, List<QuestionResult> questions) {

    public static StudentExamResultVO from(Exam exam, ExamPaper paper, StudentExamRecord record,
                                           List<ExamPaperQuestion> questions,
                                           Map<Long, StudentExamAnswer> answers) {
        return new StudentExamResultVO(exam.getId(), record.getId(), paper.getPaperName(),
                paper.getTotalScore(), record.getScore(), record.getStatus(),
                questions.stream().map(question -> QuestionResult.from(question, answers.get(question.getId()))).toList());
    }

    public record QuestionResult(
            Long paperQuestionId, String questionType, String questionContent,
            String studentAnswer, String correctAnswer, String analysisContent,
            Boolean correct, BigDecimal maxScore, BigDecimal score, String graderComment) {
        static QuestionResult from(ExamPaperQuestion question, StudentExamAnswer answer) {
            return new QuestionResult(question.getId(), question.getQuestionType(), question.getQuestionContent(),
                    answer == null ? null : answer.getAnswerContent(), question.getAnswerContent(),
                    question.getAnalysisContent(), answer == null ? null : answer.getCorrect(),
                    question.getQuestionScore(), answer == null ? BigDecimal.ZERO : answer.getScore(),
                    answer == null ? null : answer.getGraderComment());
        }
    }
}
