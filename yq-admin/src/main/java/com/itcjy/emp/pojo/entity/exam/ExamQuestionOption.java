package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题库题目选项实体类
 * <p>表示题库中选择题的选项，仅单选题和多选题拥有选项</p>
 */
@Data
@TableName("exam_question_option")
public class ExamQuestionOption {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属题目ID */
    private Long questionId;
    /** 选项标识（如 A、B、C、D） */
    private String optionKey;
    /** 选项内容 */
    private String optionContent;
    /** 选项排序序号 */
    private Integer sortOrder;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
