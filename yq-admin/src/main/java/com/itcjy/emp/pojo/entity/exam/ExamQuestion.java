package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题库题目实体类
 * <p>表示题库中的一道题目，包含题型、内容、答案、难度和状态等信息</p>
 */
@Data
@TableName("exam_question")
public class ExamQuestion {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 题型（SINGLE-单选 / MULTIPLE-多选 / JUDGE-判断 / FILL-填空 / SHORT-简答） */
    private String questionType;
    /** 题目内容 */
    private String questionContent;
    /** 标准答案 */
    private String answerContent;
    /** 答案解析 */
    private String analysisContent;
    /** 难度（EASY-简单 / NORMAL-普通 / HARD-困难） */
    private String difficulty;
    /** 状态（ENABLED-启用 / DISABLED-停用） */
    private String status;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
