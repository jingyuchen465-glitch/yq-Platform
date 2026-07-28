package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper_stage")
public class ExamPaperStage {
    @TableId(type = IdType.AUTO) private Long id;
    private Long paperId;
    private String stageName;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
