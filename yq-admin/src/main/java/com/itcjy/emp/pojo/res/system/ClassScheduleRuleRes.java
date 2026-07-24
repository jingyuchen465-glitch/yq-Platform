package com.itcjy.emp.pojo.res.system;

import java.util.List;

public record ClassScheduleRuleRes(List<Integer> classDays, List<Integer> selfStudyDays,
                                   List<Integer> restDays, boolean holidayRest) {
}
