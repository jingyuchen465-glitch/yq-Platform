package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("homework_submission")
@Schema(description = "作业提交记录")
public class HomeworkSubmission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long homeworkId;

    private Long studentId;

    private Long classId;

    private String contentObjectKey;

    private String contentFileName;

    private LocalDateTime submitTime;

    private Boolean lateSubmitted;

    private String teacherRemark;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
