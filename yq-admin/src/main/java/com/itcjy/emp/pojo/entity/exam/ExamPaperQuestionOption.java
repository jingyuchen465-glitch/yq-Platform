package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷题目选项快照实体类
 * <p>组卷时从题库复制的选择题选项快照，与试卷题目快照一一对应</p>
 */
@Data
@TableName("exam_paper_question_option")
public class ExamPaperQuestionOption {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属试卷题目快照ID */
    private Long paperQuestionId;
    /** 选项标识（如 A、B、C、D） */
    private String optionKey;
    /** 选项内容 */
    private String optionContent;
    /** 选项排序序号 */
    private Integer sortOrder;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
