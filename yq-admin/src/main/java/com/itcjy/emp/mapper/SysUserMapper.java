package com.itcjy.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE id = #{id} FOR UPDATE")
    SysUser selectByIdForUpdate(@Param("id") Long id);
}
