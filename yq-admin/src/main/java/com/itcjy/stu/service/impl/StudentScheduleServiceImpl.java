package com.itcjy.stu.service.impl;

import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.service.academic.ISysClassScheduleService;
import com.itcjy.stu.pojo.DTO.StudentScheduleQueryDTO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentScheduleCourseVO;
import com.itcjy.stu.pojo.VO.StudentScheduleVO;
import com.itcjy.stu.service.LoginService;
import com.itcjy.stu.service.StudentScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentScheduleServiceImpl implements StudentScheduleService {

    private final LoginService loginService;
    private final ISysClassScheduleService classScheduleService;

    @Override
    public StudentScheduleVO getCurrentStudentSchedule(StudentScheduleQueryDTO query) {
        StudentDetailsVO student = loginService.getCurrentStudent();
        if (student.getClassId() == null) {
            return StudentScheduleVO.unassigned();
        }

        List<StudentScheduleCourseVO> courses = classScheduleService.listSchedule(student.getClassId()).stream()
                .filter(schedule -> isWithinRange(schedule, query.getStartDate(), query.getEndDate()))
                .map(StudentScheduleCourseVO::from)
                .toList();
        return StudentScheduleVO.assigned(student.getClassId(), courses);
    }

    private boolean isWithinRange(SysClassScheduleRes schedule, LocalDate startDate, LocalDate endDate) {
        LocalDate scheduleDate = schedule.scheduleDate();
        return scheduleDate != null
                && (startDate == null || !scheduleDate.isBefore(startDate))
                && (endDate == null || !scheduleDate.isAfter(endDate));
    }
}
