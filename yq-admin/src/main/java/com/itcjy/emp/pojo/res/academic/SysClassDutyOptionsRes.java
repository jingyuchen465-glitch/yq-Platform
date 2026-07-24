package com.itcjy.emp.pojo.res.academic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "值班管理下拉选项")
public record SysClassDutyOptionsRes(
        List<CampusOption> campuses,
        List<TeacherOption> teachers,
        List<DutyTypeOption> dutyTypes
) {
    public record CampusOption(Long id, String name) {
    }

    public record TeacherOption(Long id, String name) {
    }

    public record DutyTypeOption(
            String code,
            String name,
            String startTime,
            String endTime,
            boolean classDuty
    ) {
    }
}
