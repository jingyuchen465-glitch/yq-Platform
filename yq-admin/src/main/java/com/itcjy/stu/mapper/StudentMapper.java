package com.itcjy.stu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.stu.pojo.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

}