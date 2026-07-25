package com.itcjy.emp.pojo.res.homework;

import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.SysClass;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "班级作业详情")
public record HomeworkClassStatusRes(
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "班级名称") String className,
        @Schema(description = "是否存在匹配作业") boolean published,
        @Schema(description = "匹配作业数量") int homeworkCount,
        @Schema(description = "匹配作业明细") List<HomeworkStatusItemRes> homeworks
) {
    public static HomeworkClassStatusRes from(SysClass sysClass, List<Homework> homeworks) {
        List<HomeworkStatusItemRes> items = homeworks.stream()
                .map(HomeworkStatusItemRes::from)
                .toList();
        return new HomeworkClassStatusRes(
                sysClass.getId(),
                sysClass.getClassPeriod(),
                !items.isEmpty(),
                items.size(),
                items
        );
    }
}
