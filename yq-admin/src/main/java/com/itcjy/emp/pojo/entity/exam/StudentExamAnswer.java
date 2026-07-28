package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_exam_answer")
public class StudentExamAnswer {
    @TableId(type = IdType.AUTO) private Long id;
    private Long recordId;
    private Long examId;
    private Long paperId;
    private Long paperQuestionId;
    private Long questionId;
    private String questionType;
    private BigDecimal questionScore;
    private String answerContent;
    private Boolean correct;
    private BigDecimal score;
    private Long graderUserId;
    private String graderComment;
    private LocalDateTime gradedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
