package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper_question_option")
public class ExamPaperQuestionOption {
    @TableId(type = IdType.AUTO) private Long id;
    private Long paperQuestionId;
    private String optionKey;
    private String optionContent;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
