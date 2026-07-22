package com.itcjy.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.SysClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysClassMapper extends BaseMapper<SysClass> {

    @Select("SELECT * FROM sys_class WHERE id = #{id} FOR UPDATE")
    SysClass selectByIdForUpdate(@Param("id") Long id);
}
