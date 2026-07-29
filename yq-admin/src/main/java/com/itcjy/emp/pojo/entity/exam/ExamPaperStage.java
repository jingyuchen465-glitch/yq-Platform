package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷阶段实体类
 * <p>记录试卷关联的课程阶段名称，一份试卷可包含多个阶段</p>
 */
@Data
@TableName("exam_paper_stage")
public class ExamPaperStage {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属试卷ID */
    private Long paperId;
    /** 阶段名称 */
    private String stageName;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
