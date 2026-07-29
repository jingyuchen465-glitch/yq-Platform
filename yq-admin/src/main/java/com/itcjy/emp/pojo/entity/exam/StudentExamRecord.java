package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生考试记录实体类
 * <p>记录某学生参加某场考试的整体状态、时间、成绩和批改进度</p>
 */
@Data
@TableName("student_exam_record")
public class StudentExamRecord {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属考试ID */
    private Long examId;
    /** 学生ID */
    private Long studentId;
    /** 学生开始答题时间 */
    private LocalDateTime startTime;
    /** 个人答题截止时间（开始时间 + 考试时长） */
    private LocalDateTime deadlineTime;
    /** 交卷时间 */
    private LocalDateTime submitTime;
    /** 交卷原因（MANUAL-手动交卷 / TIMEOUT-超时自动交卷 / ABSENT-缺考） */
    private String submitReason;
    /** 考试状态（NOT_STARTED / IN_PROGRESS / SUBMITTED / TIMEOUT / ABSENT） */
    private String status;
    /** 批改状态（PENDING-待批改 / GRADING-批改中 / COMPLETED-已完成） */
    private String gradingStatus;
    /** 客观题得分 */
    private BigDecimal objectiveScore;
    /** 主观题得分 */
    private BigDecimal subjectiveScore;
    /** 总分（客观题 + 主观题） */
    private BigDecimal score;
    /** 乐观锁版本号，防止并发交卷冲突 */
    @Version private Integer version;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
