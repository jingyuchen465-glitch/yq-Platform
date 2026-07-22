package com.itcjy.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysClassScheduleMapper extends BaseMapper<SysClassSchedule> {

    @Select("SELECT * FROM sys_class_schedule WHERE id = #{id} FOR UPDATE")
    SysClassSchedule selectByIdForUpdate(@Param("id") Long id);
}
