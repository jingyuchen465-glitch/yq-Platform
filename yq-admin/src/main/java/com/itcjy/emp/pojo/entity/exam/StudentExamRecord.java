package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_exam_record")
public class StudentExamRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long examId;
    private Long studentId;
    private LocalDateTime startTime;
    private LocalDateTime deadlineTime;
    private LocalDateTime submitTime;
    private String submitReason;
    private String status;
    private String gradingStatus;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal score;
    @Version private Integer version;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
