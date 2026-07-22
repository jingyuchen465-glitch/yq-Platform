package com.itcjy.emp.service.academic;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.req.academic.SysCourseDetailPageReq;
import com.itcjy.emp.pojo.req.academic.SysCourseDetailReq;
import com.itcjy.emp.pojo.req.academic.SysCourseDetailUpdateReq;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ISysCourseDetailService extends IService<SysCourseDetail> {

    /**
     * 新增课程详情
     */
    void addCourseDetail(SysCourseDetailReq req);

    /**
     * 根据 ID 删除课程详情
     */
    void deleteCourseDetail(Long id);

    /**
     * 根据 ID 列表批量删除课程详情
     */
    void deleteCourseDetails(List<Long> ids);

    /**
     * 根据 ID 更新课程详情
     */
    void updateCourseDetail(Long id, SysCourseDetailUpdateReq req);

    /**
     * 分页查询课程详情
     */
    PageResult<SysCourseDetail> pageCourseDetail(SysCourseDetailPageReq req);

    /**
     * 通过 Excel 文件批量导入课程详情
     *
     * @param courseId 课程ID
     * @param file     Excel 文件
     * @return 成功导入的记录数
     */
    int importCourseDetail(Long courseId, MultipartFile file);
}
