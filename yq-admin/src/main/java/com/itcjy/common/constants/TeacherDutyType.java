package com.itcjy.common.constants;

import com.itcjy.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TeacherDutyType {

    EVENING_STUDY_CLASS("晚自习班级值班", LocalTime.of(19, 0), LocalTime.of(21, 0), true),
    EVENING_STUDY_CAMPUS("晚自习校区统一值班", LocalTime.of(21, 0), LocalTime.of(22, 30), false),
    SELF_STUDY_CLASS("自习日班级值班", LocalTime.of(9, 0), LocalTime.of(18, 0), true);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String label;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final boolean classDuty;

    public String getStartTimeText() {
        return startTime.format(TIME_FORMATTER);
    }

    public String getEndTimeText() {
        return endTime.format(TIME_FORMATTER);
    }

    public boolean overlaps(TeacherDutyType other) {
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public static TeacherDutyType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .findFirst()
                .orElseThrow(() -> BusinessException.PARAMS_ERROR.newInstance("不支持的值班类型"));
    }
}
