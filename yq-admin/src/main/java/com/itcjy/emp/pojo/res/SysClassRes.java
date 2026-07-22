package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "班级信息")
public record SysClassRes(
        @Schema(description = "班级ID") Long id,
        @Schema(description = "班级期数") String classPeriod,
        @Schema(description = "班主任ID") Long headTeacherId,
        @Schema(description = "班主任姓名") String headTeacherName,
        @Schema(description = "校区ID") Long campusId,
        @Schema(description = "校区地点") String campusLocation,
        @Schema(description = "课程ID") Long courseId,
        @Schema(description = "课程名称") String courseName,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
