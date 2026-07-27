package com.itcjy.stu.service;

import com.itcjy.stu.pojo.VO.StudentPrepaymentOrderVO;

import java.util.List;

public interface StudentPrepaymentOrderService {

    List<StudentPrepaymentOrderVO> listCurrentStudentOrders();
}
