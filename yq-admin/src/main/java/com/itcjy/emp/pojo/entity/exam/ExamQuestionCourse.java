package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_question_course")
public class ExamQuestionCourse {
    @TableId(type = IdType.AUTO) private Long id;
    private Long questionId;
    private Long courseId;
    private String stageName;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
