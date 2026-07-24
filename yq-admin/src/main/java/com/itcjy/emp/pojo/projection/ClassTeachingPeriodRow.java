package com.itcjy.emp.pojo.projection;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClassTeachingPeriodRow {

    private Long classId;

    private String className;

    private Long campusId;

    private LocalDate teachingStartDate;

    private LocalDate teachingEndDate;

    public boolean hasSchedule() {
        return teachingStartDate != null && teachingEndDate != null;
    }

    public boolean isActiveOn(LocalDate date) {
        return hasSchedule()
                && !date.isBefore(teachingStartDate)
                && !date.isAfter(teachingEndDate);
    }
}
