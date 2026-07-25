package com.itcjy.emp.service.homework;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateCreateReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplatePageReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateUpdateReq;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateCourseOptionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateDownloadRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateMatchRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePositionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePreviewRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateRes;

import java.util.List;

public interface ICourseHomeworkTemplateService {

    PageResult<CourseHomeworkTemplateRes> page(CourseHomeworkTemplatePageReq req);

    CourseHomeworkTemplateRes create(CourseHomeworkTemplateCreateReq req);

    CourseHomeworkTemplateRes update(Long id, CourseHomeworkTemplateUpdateReq req);

    void delete(Long id);

    List<CourseHomeworkTemplateCourseOptionRes> listCourseOptions();

    List<CourseHomeworkTemplatePositionRes> listPositions(Long courseId);

    CourseHomeworkTemplatePreviewRes getPreview(Long id);

    CourseHomeworkTemplateDownloadRes getDownload(Long id);

    CourseHomeworkTemplateMatchRes findActiveByCourseDetail(Long courseDetailId);
}
