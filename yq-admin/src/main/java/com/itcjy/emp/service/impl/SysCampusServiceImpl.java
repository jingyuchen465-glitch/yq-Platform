package com.itcjy.emp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.SysCampusMapper;
import com.itcjy.emp.pojo.entity.SysCampus;
import com.itcjy.emp.pojo.req.SysCampusPageReq;
import com.itcjy.emp.pojo.req.SysCampusReq;
import com.itcjy.emp.pojo.req.SysCampusUpdateReq;
import com.itcjy.emp.service.ISysCampusService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SysCampusServiceImpl extends ServiceImpl<SysCampusMapper, SysCampus> implements ISysCampusService {

    @Override
    public void addCampus(SysCampusReq req) {
        SysCampus campus = BeanUtil.copyProperties(req, SysCampus.class);
        this.save(campus);
    }

    @Override
    public void deleteCampus(Long id) {
        SysCampus campus = this.getById(id);
        if (campus == null) {
            throw BusinessException.CAMPUS_NOT_EXIST.newInstance("校区不存在");
        }
        this.removeById(id);
    }

    @Override
    public void updateCampus(Long id, SysCampusUpdateReq req) {
        SysCampus campus = this.getById(id);
        if (campus == null) {
            throw BusinessException.CAMPUS_NOT_EXIST.newInstance("校区不存在");
        }
        SysCampus updateCampus = BeanUtil.copyProperties(req, SysCampus.class);
        updateCampus.setId(id);
        this.updateById(updateCampus);
    }

    @Override
    public PageResult<SysCampus> pageCampus(SysCampusPageReq req) {
        IPage<SysCampus> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysCampus>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getCampusLocation()), SysCampus::getCampusLocation, req.getCampusLocation())
                        .like(StrUtil.isNotBlank(req.getManagerName()), SysCampus::getManagerName, req.getManagerName())
                        .orderByDesc(SysCampus::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
