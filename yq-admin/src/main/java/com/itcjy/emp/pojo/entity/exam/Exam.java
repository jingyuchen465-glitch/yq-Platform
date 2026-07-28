package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO) private Long id;
    private Long paperId;
    private Long classId;
    private LocalDateTime startTime;
    private LocalDateTime entryDeadlineTime;
    private Integer durationMinutes;
    private LocalDateTime closeTime;
    private Long invigilatorUserId;
    private Boolean answerVisible;
    private LocalDateTime answerVisibleAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
