package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_timeout_outbox")
public class ExamTimeoutOutbox {
    @TableId(type = IdType.AUTO) private Long id;
    private Long recordId;
    private LocalDateTime deadlineTime;
    private String status;
    private Integer publishAttempts;
    private LocalDateTime publishedAt;
    private LocalDateTime processedAt;
    private String lastError;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
