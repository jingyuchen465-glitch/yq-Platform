package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试实体类
 * <p>表示一场已发布的考试，关联试卷和班级，包含考试时间安排和答卷可见性配置</p>
 */
@Data
@TableName("exam")
public class Exam {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 关联试卷ID */
    private Long paperId;
    /** 关联班级ID */
    private Long classId;
    /** 考试开始时间 */
    private LocalDateTime startTime;
    /** 最晚入场时间，超过此时间不允许进入考试 */
    private LocalDateTime entryDeadlineTime;
    /** 考试时长（分钟） */
    private Integer durationMinutes;
    /** 考试关闭时间（最晚入场时间 + 考试时长） */
    private LocalDateTime closeTime;
    /** 监考老师用户ID */
    private Long invigilatorUserId;
    /** 是否对学生公布答卷结果 */
    private Boolean answerVisible;
    /** 答卷结果公布时间 */
    private LocalDateTime answerVisibleAt;
    /** 创建人（发布考试的管理员ID） */
    private Long createdBy;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
