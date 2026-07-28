package com.itcjy.emp.pojo.res.exam;

import com.itcjy.emp.pojo.entity.exam.ExamQuestion;
import com.itcjy.emp.pojo.entity.exam.ExamQuestionCourse;
import com.itcjy.emp.pojo.entity.exam.ExamQuestionOption;
import java.time.LocalDateTime;
import java.util.List;

public record ExamQuestionRes(
        Long id,
        String questionType,
        String questionContent,
        String answerContent,
        String analysisContent,
        String difficulty,
        String status,
        List<OptionItem> options,
        List<CourseStageItem> courseStages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ExamQuestionRes from(ExamQuestion question,
                                       List<ExamQuestionOption> options,
                                       List<ExamQuestionCourse> courses) {
        return new ExamQuestionRes(
                question.getId(), question.getQuestionType(), question.getQuestionContent(),
                question.getAnswerContent(), question.getAnalysisContent(), question.getDifficulty(),
                question.getStatus(),
                options.stream().map(OptionItem::from).toList(),
                courses.stream().map(CourseStageItem::from).toList(),
                question.getCreatedAt(), question.getUpdatedAt());
    }

    public record OptionItem(Long id, String optionKey, String optionContent, Integer sortOrder) {
        static OptionItem from(ExamQuestionOption option) {
            return new OptionItem(option.getId(), option.getOptionKey(), option.getOptionContent(), option.getSortOrder());
        }
    }

    public record CourseStageItem(Long id, Long courseId, String stageName) {
        static CourseStageItem from(ExamQuestionCourse relation) {
            return new CourseStageItem(relation.getId(), relation.getCourseId(), relation.getStageName());
        }
    }
}
