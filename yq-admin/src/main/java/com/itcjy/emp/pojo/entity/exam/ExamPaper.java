package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷实体类
 * <p>表示一份考试试卷，包含试卷名称、关联课程、总分和状态</p>
 */
@Data
@TableName("exam_paper")
public class ExamPaper {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 试卷名称 */
    private String paperName;
    /** 关联课程ID */
    private Long courseId;
    /** 试卷总分 */
    private BigDecimal totalScore;
    /** 试卷状态（DRAFT-草稿 / LOCKED-已锁定 / ARCHIVED-已归档） */
    private String status;
    /** 创建人（组卷管理员ID） */
    private Long createdBy;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
