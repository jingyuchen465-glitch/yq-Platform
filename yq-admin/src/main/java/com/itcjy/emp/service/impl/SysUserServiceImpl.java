package com.itcjy.emp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.emp.mapper.SysUserMapper;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.service.ISysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
}
