package com.itcjy.emp.pojo.res.market;

import com.itcjy.emp.pojo.entity.SysCourse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "产品可选课程")
public record MarketCourseOptionRes(
        @Schema(description = "课程ID") Long id,
        @Schema(description = "课程名称") String courseName,
        @Schema(description = "课程天数") Integer courseDays,
        @Schema(description = "上课方式") String teachingMode
) {
    public static MarketCourseOptionRes from(SysCourse course) {
        return new MarketCourseOptionRes(
                course.getId(),
                course.getCourseName(),
                course.getCourseDays(),
                course.getTeachingMode()
        );
    }
}
