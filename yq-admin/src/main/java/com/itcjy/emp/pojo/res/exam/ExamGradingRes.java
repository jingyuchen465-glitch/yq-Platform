package com.itcjy.emp.pojo.res.exam;

import com.itcjy.emp.pojo.entity.exam.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ExamGradingRes(
        ExamRecordRes record,
        List<SubjectiveAnswer> answers) {
    public record SubjectiveAnswer(
            Long answerId, Long paperQuestionId, String questionType, String questionContent,
            String referenceAnswer, String analysisContent, String studentAnswer,
            BigDecimal maxScore, BigDecimal score, String graderComment) {
        public static SubjectiveAnswer from(StudentExamAnswer answer, ExamPaperQuestion question) {
            return new SubjectiveAnswer(answer.getId(), answer.getPaperQuestionId(), answer.getQuestionType(),
                    question.getQuestionContent(), question.getAnswerContent(), question.getAnalysisContent(),
                    answer.getAnswerContent(), answer.getQuestionScore(), answer.getScore(), answer.getGraderComment());
        }
    }
}
