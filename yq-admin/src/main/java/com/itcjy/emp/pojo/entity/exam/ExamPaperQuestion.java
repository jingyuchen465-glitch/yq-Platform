package com.itcjy.emp.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷题目快照实体类
 * <p>组卷时从题库复制题目信息形成的快照，保证试卷内容不受题库后续修改影响</p>
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {
    /** 主键ID */
    @TableId(type = IdType.AUTO) private Long id;
    /** 所属试卷ID */
    private Long paperId;
    /** 原始题库题目ID */
    private Long questionId;
    /** 题型（SINGLE-单选 / MULTIPLE-多选 / JUDGE-判断 / FILL-填空 / SHORT-简答） */
    private String questionType;
    /** 题目内容快照 */
    private String questionContent;
    /** 标准答案快照 */
    private String answerContent;
    /** 答案解析快照 */
    private String analysisContent;
    /** 难度（EASY-简单 / NORMAL-普通 / HARD-困难） */
    private String difficulty;
    /** 本题分值 */
    private BigDecimal questionScore;
    /** 题目在试卷中的排序序号 */
    private Integer sortOrder;
    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
