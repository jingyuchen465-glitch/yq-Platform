package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目课程关联实体类
 * <p>记录题目与课程及课程阶段的关联关系，一道题可关联多个课程阶段</p>
 */
@Data
@TableName("exam_question_course")
public class ExamQuestionCourse {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 题目ID */
    private Long questionId;
    /** 课程ID */
    private Long courseId;
    /** 课程阶段名称 */
    private String stageName;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
