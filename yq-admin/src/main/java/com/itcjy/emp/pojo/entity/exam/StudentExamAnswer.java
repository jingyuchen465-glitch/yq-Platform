package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生答题记录实体类
 * <p>记录学生对某道题目的作答内容、判分结果和批改信息</p>
 */
@Data
@TableName("student_exam_answer")
public class StudentExamAnswer {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属考试记录ID */
    private Long recordId;
    /** 所属考试ID */
    private Long examId;
    /** 所属试卷ID */
    private Long paperId;
    /** 试卷题目快照ID */
    private Long paperQuestionId;
    /** 原始题库题目ID */
    private Long questionId;
    /** 题型 */
    private String questionType;
    /** 题目分值 */
    private BigDecimal questionScore;
    /** 学生作答内容 */
    private String answerContent;
    /** 客观题是否回答正确（主观题由老师批改后确定） */
    private Boolean correct;
    /** 得分 */
    private BigDecimal score;
    /** 批改老师用户ID */
    private Long graderUserId;
    /** 批改评语 */
    private String graderComment;
    /** 批改时间 */
    private LocalDateTime gradedAt;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
