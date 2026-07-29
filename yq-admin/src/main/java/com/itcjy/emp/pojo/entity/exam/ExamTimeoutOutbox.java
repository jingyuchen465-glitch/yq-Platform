package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试超时发件箱实体类
 * <p>基于事务性发件箱模式（Transactional Outbox）记录考试超时消息的发送状态，
 * 确保超时交卷消息可靠投递到 RocketMQ</p>
 */
@Data
@TableName("exam_timeout_outbox")
public class ExamTimeoutOutbox {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 关联的学生考试记录ID */
    private Long recordId;
    /** 答题截止时间 */
    private LocalDateTime deadlineTime;
    /** 消息状态（PENDING-待发送 / SENT-已发送 / PROCESSED-已处理） */
    private String status;
    /** 发布尝试次数 */
    private Integer publishAttempts;
    /** 消息成功发布时间 */
    private LocalDateTime publishedAt;
    /** 消息被消费处理的时间 */
    private LocalDateTime processedAt;
    /** 最后一次发送失败的错误信息 */
    private String lastError;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
