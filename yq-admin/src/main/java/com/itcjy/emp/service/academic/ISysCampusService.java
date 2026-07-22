package com.itcjy.emp.service.academic;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCampus;
import com.itcjy.emp.pojo.req.academic.SysCampusPageReq;
import com.itcjy.emp.pojo.req.academic.SysCampusReq;
import com.itcjy.emp.pojo.req.academic.SysCampusUpdateReq;

public interface ISysCampusService extends IService<SysCampus> {

    /**
     * 新增校区
     */
    void addCampus(SysCampusReq req);

    /**
     * 根据 ID 删除校区
     */
    void deleteCampus(Long id);

    /**
     * 根据 ID 更新校区信息
     */
    void updateCampus(Long id, SysCampusUpdateReq req);

    /**
     * 分页查询校区
     */
    PageResult<SysCampus> pageCampus(SysCampusPageReq req);
}
