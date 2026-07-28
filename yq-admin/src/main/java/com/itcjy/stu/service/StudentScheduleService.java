package com.itcjy.stu.service;

import com.itcjy.stu.pojo.DTO.StudentScheduleQueryDTO;
import com.itcjy.stu.pojo.VO.StudentScheduleVO;

public interface StudentScheduleService {

    StudentScheduleVO getCurrentStudentSchedule(StudentScheduleQueryDTO query);
}
