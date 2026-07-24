package com.itcjy.emp.pojo.res.academic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "校区每日值班安排")
public record SysClassDutyDailyRes(
        LocalDate dutyDate,
        Long campusId,
        String campusName,
        String dutyMode,
        List<Long> busyTeacherIds,
        DutyAssignment campusDuty,
        List<ClassDutyRow> eveningClassDuties,
        List<ClassDutyRow> selfStudyClassDuties,
        List<UnscheduledClass> unscheduledClasses
) {
    public record DutyAssignment(
            Long id,
            String dutyType,
            String dutyTypeName,
            String startTime,
            String endTime,
            Long teacherId,
            String teacherName,
            String remark
    ) {
    }

    public record ClassDutyRow(
            Long classId,
            String className,
            LocalDate teachingStartDate,
            LocalDate teachingEndDate,
            DutyAssignment assignment
    ) {
    }

    public record UnscheduledClass(Long classId, String className) {
    }
}
