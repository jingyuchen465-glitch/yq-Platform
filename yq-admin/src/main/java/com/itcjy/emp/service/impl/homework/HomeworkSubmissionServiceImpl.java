package com.itcjy.emp.service.impl.homework;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.homework.HomeworkMapper;
import com.itcjy.emp.mapper.homework.HomeworkSubmissionMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionGradeReq;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionItemRes;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionOverviewRes;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.service.homework.IHomeworkSubmissionService;
import com.itcjy.emp.service.oss.IOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeworkSubmissionServiceImpl
        extends ServiceImpl<HomeworkSubmissionMapper, HomeworkSubmission>
        implements IHomeworkSubmissionService {

    private final HomeworkMapper homeworkMapper;
    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final IOssService ossService;

    @Override
    @Transactional(readOnly = true)
    public HomeworkSubmissionOverviewRes pageByHomework(Long homeworkId, HomeworkSubmissionPageReq req) {
        Homework homework = requireHomework(homeworkId);
        SysClass sysClass = sysClassMapper.selectById(homework.getClassId());

        LambdaQueryWrapper<HomeworkSubmission> pageQuery = buildPageQuery(homeworkId, req);
        IPage<HomeworkSubmission> page = page(
                new Page<>(req.getCurrent(), req.getSize()),
                pageQuery.orderByDesc(HomeworkSubmission::getSubmitTime)
                        .orderByDesc(HomeworkSubmission::getId)
        );

        Map<Long, SysUser> students = loadStudents(page.getRecords());
        List<HomeworkSubmissionItemRes> records = page.getRecords().stream()
                .map(item -> HomeworkSubmissionItemRes.from(item, students.get(item.getStudentId())))
                .toList();

        long submissionCount = count(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId));
        long reviewedCount = count(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                .isNotNull(HomeworkSubmission::getScore));
        long lateCount = count(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                .eq(HomeworkSubmission::getLateSubmitted, true));

        return HomeworkSubmissionOverviewRes.from(
                homework,
                sysClass,
                submissionCount,
                reviewedCount,
                lateCount,
                new PageResult<>(page.getTotal(), records)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OssDownloadUrlRes generateDownloadUrl(Long submissionId, boolean preview) {
        HomeworkSubmission submission = requireSubmission(submissionId);
        return ossService.generateDownloadUrl(submission.getContentObjectKey(), preview,
                submission.getContentFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeworkSubmissionItemRes grade(Long submissionId, HomeworkSubmissionGradeReq req) {
        HomeworkSubmission submission = requireSubmission(submissionId);

        // 使用定向 UPDATE，数据库层面只写入允许教师修改的两个字段。
        boolean updated = lambdaUpdate()
                .eq(HomeworkSubmission::getId, submissionId)
                .set(HomeworkSubmission::getScore, req.score())
                .set(HomeworkSubmission::getTeacherRemark, StrUtil.trim(req.teacherRemark()))
                .update();
        if (!updated) {
            throw BusinessException.DATA_ERROR.newInstance("作业批改保存失败");
        }

        HomeworkSubmission graded = requireSubmission(submissionId);
        SysUser student = sysUserMapper.selectById(graded.getStudentId());
        return HomeworkSubmissionItemRes.from(graded, student);
    }

    private LambdaQueryWrapper<HomeworkSubmission> buildPageQuery(
            Long homeworkId,
            HomeworkSubmissionPageReq req) {
        LambdaQueryWrapper<HomeworkSubmission> query = Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId);
        if (req.getReviewed() != null) {
            if (Boolean.TRUE.equals(req.getReviewed())) {
                query.isNotNull(HomeworkSubmission::getScore);
            } else {
                query.isNull(HomeworkSubmission::getScore);
            }
        }
        query.eq(req.getLateSubmitted() != null,
                HomeworkSubmission::getLateSubmitted,
                req.getLateSubmitted());

        if (StrUtil.isNotBlank(req.getStudentKeyword())) {
            List<Long> studentIds = findStudentIds(req.getStudentKeyword().trim());
            if (studentIds.isEmpty()) {
                query.eq(HomeworkSubmission::getStudentId, -1L);
            } else {
                query.in(HomeworkSubmission::getStudentId, studentIds);
            }
        }
        return query;
    }

    private List<Long> findStudentIds(String keyword) {
        return sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getId)
                        .and(query -> query.like(SysUser::getRealName, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getUsername, keyword)))
                .stream()
                .map(SysUser::getId)
                .toList();
    }

    private Map<Long, SysUser> loadStudents(List<HomeworkSubmission> submissions) {
        if (submissions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> studentIds = submissions.stream()
                .map(HomeworkSubmission::getStudentId)
                .distinct()
                .toList();
        return sysUserMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private Homework requireHomework(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw BusinessException.HOMEWORK_NOT_EXIST.newInstance("作业不存在");
        }
        return homework;
    }

    private HomeworkSubmission requireSubmission(Long submissionId) {
        HomeworkSubmission submission = getById(submissionId);
        if (submission == null) {
            throw BusinessException.HOMEWORK_SUBMISSION_NOT_EXIST.newInstance("学生作业提交记录不存在");
        }
        return submission;
    }
}
