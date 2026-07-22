package com.itcjy.emp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.TeachingModeEnum;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.SysClassMapper;
import com.itcjy.emp.pojo.entity.SysCampus;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.SysClassPageReq;
import com.itcjy.emp.pojo.req.SysClassReq;
import com.itcjy.emp.pojo.req.SysClassUpdateReq;
import com.itcjy.emp.pojo.res.SysClassFormOptionsRes;
import com.itcjy.emp.pojo.res.SysClassOptionRes;
import com.itcjy.emp.pojo.res.SysClassRes;
import com.itcjy.emp.service.ISysCampusService;
import com.itcjy.emp.service.ISysClassService;
import com.itcjy.emp.service.ISysCourseService;
import com.itcjy.emp.service.ISysRoleService;
import com.itcjy.emp.service.ISysUserRoleService;
import com.itcjy.emp.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements ISysClassService {

    private static final String COORDINATOR_ROLE_CODE = "COORDINATOR";//班主任

    private final ISysCampusService sysCampusService;
    private final ISysCourseService sysCourseService;
    private final ISysUserService sysUserService;
    private final ISysRoleService sysRoleService;
    private final ISysUserRoleService sysUserRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addClass(SysClassReq req) {
        validateReferences(req.getCampusId(), req.getHeadTeacherId(), req.getCourseId());
        SysClass sysClass = new SysClass();
        fillClass(sysClass, req.getClassPeriod(), req.getHeadTeacherId(), req.getCampusId(), req.getCourseId());
        this.save(sysClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClass(Long id, SysClassUpdateReq req) {
        checkClassExists(id);
        validateReferences(req.getCampusId(), req.getHeadTeacherId(), req.getCourseId());
        SysClass sysClass = new SysClass();
        sysClass.setId(id);
        fillClass(sysClass, req.getClassPeriod(), req.getHeadTeacherId(), req.getCampusId(), req.getCourseId());
        this.updateById(sysClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long id) {
        checkClassExists(id);
        this.removeById(id);
    }

    @Override
    public SysClassRes getClassDetail(Long id) {
        SysClass sysClass = this.getById(id);
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        return toResponses(List.of(sysClass)).get(0);
    }

    @Override
    public PageResult<SysClassRes> pageClasses(SysClassPageReq req) {
        IPage<SysClass> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysClass>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getClassPeriod()), SysClass::getClassPeriod, req.getClassPeriod())
                        .eq(req.getHeadTeacherId() != null, SysClass::getHeadTeacherId, req.getHeadTeacherId())
                        .eq(req.getCampusId() != null, SysClass::getCampusId, req.getCampusId())
                        .eq(req.getCourseId() != null, SysClass::getCourseId, req.getCourseId())
                        .orderByDesc(SysClass::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(page.getTotal(), toResponses(page.getRecords()));
    }

    @Override
    public SysClassFormOptionsRes listFormOptions() {
        List<SysClassOptionRes> campuses = sysCampusService.lambdaQuery()
                .orderByAsc(SysCampus::getCampusLocation)
                .list()
                .stream()
                .map(campus -> new SysClassOptionRes(campus.getId(), campus.getCampusLocation()))
                .toList();

        List<SysClassOptionRes> headTeachers = listCoordinatorOptions();

        List<SysClassOptionRes> courses = sysCourseService.lambdaQuery()
                .eq(SysCourse::getTeachingMode, TeachingModeEnum.OFFLINE.name())
                .orderByAsc(SysCourse::getCourseName)
                .list()
                .stream()
                .map(course -> new SysClassOptionRes(course.getId(), course.getCourseName()))
                .toList();
        return new SysClassFormOptionsRes(campuses, headTeachers, courses);
    }

    private List<SysClassOptionRes> listCoordinatorOptions() {
        SysRole coordinatorRole = findCoordinatorRole();
        if (coordinatorRole == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getRoleId, coordinatorRole.getId())
                .list()
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysUserService.listByIds(userIds).stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(user -> new SysClassOptionRes(user.getId(), getUserDisplayName(user)))
                .toList();
    }

    private List<SysClassRes> toResponses(List<SysClass> classes) {
        Map<Long, SysCampus> campusMap = toMap(
                sysCampusService.listByIds(classes.stream().map(SysClass::getCampusId).distinct().toList()),
                SysCampus::getId
        );
        Map<Long, SysUser> userMap = toMap(
                sysUserService.listByIds(classes.stream().map(SysClass::getHeadTeacherId).distinct().toList()),
                SysUser::getId
        );
        Map<Long, SysCourse> courseMap = toMap(
                sysCourseService.listByIds(classes.stream().map(SysClass::getCourseId).distinct().toList()),
                SysCourse::getId
        );
        return classes.stream().map(sysClass -> {
            SysCampus campus = campusMap.get(sysClass.getCampusId());
            SysUser headTeacher = userMap.get(sysClass.getHeadTeacherId());
            SysCourse course = courseMap.get(sysClass.getCourseId());
            return new SysClassRes(
                    sysClass.getId(),
                    sysClass.getClassPeriod(),
                    sysClass.getHeadTeacherId(),
                    headTeacher == null ? null : getUserDisplayName(headTeacher),
                    sysClass.getCampusId(),
                    campus == null ? null : campus.getCampusLocation(),
                    sysClass.getCourseId(),
                    course == null ? null : course.getCourseName(),
                    sysClass.getCreatedAt(),
                    sysClass.getUpdatedAt()
            );
        }).toList();
    }

    private <T> Map<Long, T> toMap(List<T> items, Function<T, Long> idGetter) {
        return items.stream().collect(Collectors.toMap(idGetter, Function.identity()));
    }

    private void validateReferences(Long campusId, Long headTeacherId, Long courseId) {
        if (sysCampusService.getById(campusId) == null) {
            throw BusinessException.CAMPUS_NOT_EXIST.newInstance("所选校区不存在");
        }

        SysUser headTeacher = sysUserService.getById(headTeacherId);
        if (headTeacher == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("所选班主任不存在");
        }
        SysRole coordinatorRole = findCoordinatorRole();
        if (coordinatorRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("班主任角色COORDINATOR不存在");
        }
        boolean isCoordinator = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, headTeacherId)
                .eq(SysUserRole::getRoleId, coordinatorRole.getId())
                .exists();
        if (!isCoordinator) {
            throw BusinessException.DATA_ERROR.newInstance("所选用户不是班主任角色");
        }

        SysCourse course = sysCourseService.getById(courseId);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("所选课程不存在");
        }
        if (!TeachingModeEnum.OFFLINE.name().equals(course.getTeachingMode())) {
            throw BusinessException.DATA_ERROR.newInstance("班级只能选择线下课程");
        }
    }

    private SysRole findCoordinatorRole() {
        return sysRoleService.lambdaQuery()
                .eq(SysRole::getRoleCode, COORDINATOR_ROLE_CODE)
                .one();
    }

    private void checkClassExists(Long id) {
        if (this.getById(id) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
    }

    private void fillClass(SysClass sysClass, String classPeriod, Long headTeacherId, Long campusId, Long courseId) {
        sysClass.setClassPeriod(classPeriod.trim());
        sysClass.setHeadTeacherId(headTeacherId);
        sysClass.setCampusId(campusId);
        sysClass.setCourseId(courseId);
    }

    private String getUserDisplayName(SysUser user) {
        String name = StrUtil.isNotBlank(user.getRealName())
                ? user.getRealName()
                : StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        if (StrUtil.isNotBlank(user.getUsername()) && !Objects.equals(name, user.getUsername())) {
            return name + "（" + user.getUsername() + "）";
        }
        return name;
    }
}
