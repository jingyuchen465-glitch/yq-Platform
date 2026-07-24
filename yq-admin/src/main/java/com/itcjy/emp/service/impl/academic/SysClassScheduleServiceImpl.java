package com.itcjy.emp.service.impl.academic;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.HolidayInfo;
import com.itcjy.common.utils.HolidayUtil;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.academic.SysClassScheduleMapper;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.mapper.system.SysUserRoleMapper;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignmentOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseRes;
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import com.itcjy.emp.service.academic.ISysClassScheduleService;
import com.itcjy.emp.service.system.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 班级课程表服务实现类。
 * <p>
 * 负责班级课表的完整生命周期管理，包括：
 * <ul>
 *     <li>根据排课规则自动生成课表（上课日/自习日/休息日/节假日）</li>
 *     <li>按课程阶段批量分配授课教师</li>
 *     <li>临时加课并自动顺延后续课程</li>
 *     <li>删除课程日程并前移后续课程</li>
 *     <li>教师时间冲突校验（同一教师同一天只能在一个班级授课）</li>
 * </ul>
 * <p>
 * 并发安全策略：写操作通过 SELECT ... FOR UPDATE 对班级行、教师行、课表行加排他锁，
 * 配合乐观快照比对（ScheduleSnapshot）防止并发修改导致数据不一致。
 */
@Service
@RequiredArgsConstructor
public class SysClassScheduleServiceImpl extends ServiceImpl<SysClassScheduleMapper, SysClassSchedule>
        implements ISysClassScheduleService {

    /** 课表生成/顺延的最大天数上限，防止排课规则配置异常导致无限循环 */
    private static final int MAX_GENERATION_DAYS = 10_000;
    /** 讲师角色编码，用于查询具有授课资格的用户 */
    private static final String LECTURER_ROLE_CODE = "LECTURER";
    /** 临时加课的默认课程内容描述 */
    private static final String TEMPORARY_COURSE_CONTENT = "临时加课";

    private final SysClassMapper sysClassMapper;
    private final SysCourseDetailMapper sysCourseDetailMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ISysConfigService sysConfigService;
    private final HolidayUtil holidayUtil;
    /** 编程式事务模板，用于需要在加锁后再执行事务的场景 */
    private final TransactionTemplate transactionTemplate;

    /**
     * 生成班级课表。
     * <p>
     * 根据班级关联的课程详情列表和全局排课规则，从指定起始日期开始逐日生成课表，
     * 直到所有课程详情消耗完毕。生成前会校验起始日期的合法性（非节假日、属于上课日），
     * 生成后以事务方式替换该班级原有课表。
     *
     * @param req 包含班级ID和起始上课日期
     * @return 生成结果摘要（起止日期、总天数、课程节数等）
     */
    @Override
    public SysClassScheduleGenerateRes generateSchedule(SysClassScheduleGenerateReq req) {
        SysClass sysClass = sysClassMapper.selectById(req.classId());
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        // 加载全局排课规则（上课日/自习日/休息日/节假日是否休息）
        ScheduleRule rule = loadRule();
        // 校验起始日期：不能是法定节假日
        HolidayInfo firstDayHoliday = getHolidayInfo(req.startDate());
        if (isHoliday(firstDayHoliday)) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("第一天上课日期不能是法定节假日");
        }
        // 校验起始日期：必须是排课规则中允许的上课日（星期几）
        if (!rule.classDays().contains(req.startDate().getDayOfWeek().getValue())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("第一天上课日期不在允许的上课日中");
        }

        // 查询班级关联课程的所有课程详情，按天序号排序
        List<SysCourseDetail> courseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId)
        );
        if (courseDetails.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级关联课程暂无课程详情，无法生成课表");
        }

        // 按排课规则逐日构建课表，然后事务性替换旧课表
        List<SysClassSchedule> schedules = buildSchedules(sysClass, req.startDate(), courseDetails, rule);
        replaceSchedules(sysClass.getId(), schedules);

        LocalDate endDate = schedules.get(schedules.size() - 1).getScheduleDate();
        return new SysClassScheduleGenerateRes(
                sysClass.getId(),
                sysClass.getClassPeriod(),
                req.startDate(),
                endDate,
                schedules.size(),
                courseDetails.size()
        );
    }

    /**
     * 事务性替换指定班级的全部课表：先删除旧课表，再批量插入新课表。
     */
    private void replaceSchedules(Long classId, List<SysClassSchedule> schedules) {
        transactionTemplate.executeWithoutResult(status -> {
            this.remove(Wrappers.<SysClassSchedule>lambdaQuery()
                    .eq(SysClassSchedule::getClassId, classId));
            if (!this.saveBatch(schedules)) {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程表保存失败");
            }
        });
    }

    /**
     * 查询指定班级的完整课表列表，按日期升序排列。
     * <p>
     * 批量加载课程详情和教师信息，避免 N+1 查询。
     *
     * @param classId 班级ID
     * @return 课表响应列表（含阶段名称、教师姓名）
     */
    @Override
    public List<SysClassScheduleRes> listSchedule(Long classId) {
        if (sysClassMapper.selectById(classId) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        List<SysClassSchedule> schedules = this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId)
                .orderByAsc(SysClassSchedule::getScheduleDate));
        // 批量查询课程详情，构建 ID -> 详情 映射
        List<Long> courseDetailIds = schedules.stream()
                .map(SysClassSchedule::getCourseDetailId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, SysCourseDetail> courseDetailMap = courseDetailIds.isEmpty()
                ? Map.of()
                : sysCourseDetailMapper.selectBatchIds(courseDetailIds).stream()
                .collect(Collectors.toMap(SysCourseDetail::getId, Function.identity()));
        // 批量查询教师信息，构建 ID -> 用户 映射
        Map<Long, SysUser> teacherMap = loadUserMap(schedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        // 组装响应：关联阶段名称和教师显示名
        return schedules.stream()
                .map(schedule -> {
                    SysCourseDetail detail = schedule.getCourseDetailId() == null
                            ? null
                            : courseDetailMap.get(schedule.getCourseDetailId());
                    String stageName = detail == null ? null : detail.getStageName();
                    SysUser teacher = schedule.getTeacherId() == null
                            ? null
                            : teacherMap.get(schedule.getTeacherId());
                    String teacherName = teacher == null ? null : getUserDisplayName(teacher);
                    return SysClassScheduleRes.from(schedule, stageName, teacherName);
                })
                .toList();
    }

    /**
     * 查询单条课程日程详情。
     *
     * @param scheduleId 课程日程ID
     * @return 课程日程响应（含阶段名称、教师姓名）
     */
    @Override
    public SysClassScheduleRes getSchedule(Long scheduleId) {
        return toScheduleResponse(requireSchedule(scheduleId));
    }

    /**
     * 修改单条课程日程（更换教师、课程阶段、课程内容）。
     * <p>
     * 加锁顺序：班级行 → 教师行 → 课表行，防止并发修改。
     * 若课程阶段发生变化，会将 courseDetailId 更新为新阶段的锚点记录。
     *
     * @param scheduleId 课程日程ID
     * @param req        修改请求（教师ID、阶段名称、课程内容）
     * @return 修改后的课程日程响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysClassScheduleRes updateSchedule(Long scheduleId, SysClassScheduleUpdateReq req) {
        // 前置校验：日程必须存在且为上课类型
        SysClassSchedule currentSchedule = requireSchedule(scheduleId);
        requireTeachingSchedule(currentSchedule);
        // 锁定班级行，防止并发修改课表结构
        SysClass lockedClass = sysClassMapper.selectByIdForUpdate(currentSchedule.getClassId());
        if (lockedClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        // 锁定教师行并校验讲师资格
        SysUser teacher = sysUserMapper.selectByIdForUpdate(req.teacherId());
        validateLecturer(teacher);
        // 锁定课表行，二次校验状态一致性
        SysClassSchedule lockedSchedule = baseMapper.selectByIdForUpdate(scheduleId);
        if (lockedSchedule == null) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }
        requireTeachingSchedule(lockedSchedule);
        if (!Objects.equals(lockedSchedule.getClassId(), lockedClass.getId())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课程日程所属班级已发生变化，请刷新后重试");
        }

        // 若阶段名称变更，将 courseDetailId 指向新阶段的锚点记录
        String stageName = req.stageName().trim();
        SysCourseDetail currentDetail = sysCourseDetailMapper.selectById(lockedSchedule.getCourseDetailId());
        String currentStageName = currentDetail == null ? null : currentDetail.getStageName();
        if (!Objects.equals(currentStageName, stageName)) {
            SysCourseDetail stageAnchor = findStageAnchor(lockedClass.getCourseId(), stageName);
            lockedSchedule.setCourseDetailId(stageAnchor.getId());
        }

        // 校验教师在该日期是否已被其他班级占用（排除当前日程自身）
        validateTeacherAvailability(
                teacher.getId(),
                List.of(lockedSchedule.getScheduleDate()),
                List.of(lockedSchedule.getId()));
        lockedSchedule.setTeacherId(teacher.getId());
        lockedSchedule.setCourseContent(req.courseContent().trim());
        if (baseMapper.updateById(lockedSchedule) <= 0) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程日程修改失败");
        }
        return SysClassScheduleRes.from(lockedSchedule, stageName, getUserDisplayName(teacher));
    }

    /**
     * 删除单条课程日程。
     * <p>
     * 删除策略：
     * <ul>
     *     <li>若目标日期是节假日，则将该日程还原为节假日类型（清空教师和课程信息）</li>
     *     <li>若目标日期是普通上课日，则将后续所有上课日程的课程内容依次前移，
     *         并删除尾部多余的日程记录</li>
     * </ul>
     * 实际写操作在事务中通过加锁 + 快照比对保证并发安全。
     *
     * @param scheduleId 要删除的课程日程ID
     */
    @Override
    public void deleteSchedule(Long scheduleId) {
        SysClassSchedule currentSchedule = requireSchedule(scheduleId);
        requireTeachingSchedule(currentSchedule);
        HolidayInfo holidayInfo = getHolidayInfo(currentSchedule.getScheduleDate());
        List<SysClassSchedule> schedules = listClassSchedules(currentSchedule.getClassId());
        // 构建删除计划（无锁阶段，仅计算需要更新/删除的记录）
        ScheduleDeletePlan plan = buildScheduleDeletePlan(currentSchedule, schedules, holidayInfo);
        // 在事务中加锁执行删除计划
        transactionTemplate.executeWithoutResult(status -> executeScheduleDeletePlan(plan));
    }

    /**
     * 构建课程日程删除计划（无锁阶段）。
     * <p>
     * 根据目标日程所在日期是否为节假日，分两种策略：
     * <ul>
     *     <li>节假日：将该日程还原为节假日类型，清空教师和课程关联</li>
     *     <li>普通日：将目标位置之后的所有上课日程内容依次前移一位，
     *         然后删除尾部多余的日程（含非上课日）</li>
     * </ul>
     *
     * @param targetSchedule 要删除的目标日程
     * @param schedules      该班级的全部课表（按日期排序）
     * @param holidayInfo    目标日期的节假日信息
     * @return 删除计划（包含更新列表、删除ID列表、教师分配校验列表）
     */
    private ScheduleDeletePlan buildScheduleDeletePlan(
            SysClassSchedule targetSchedule,
            List<SysClassSchedule> schedules,
            HolidayInfo holidayInfo) {
        List<SysClassSchedule> updates = new ArrayList<>();
        List<Long> deleteIds = new ArrayList<>();
        List<TeacherDateAssignment> teacherAssignments = new ArrayList<>();

        // 策略一：目标日期是节假日 → 还原为节假日类型
        if (isHoliday(holidayInfo)) {
            SysClassSchedule holidaySchedule = copySchedule(targetSchedule);
            holidaySchedule.setTeacherId(null);
            holidaySchedule.setCourseDetailId(null);
            holidaySchedule.setCourseContent(StrUtil.isNotBlank(holidayInfo.getName())
                    ? holidayInfo.getName()
                    : "法定节假日");
            holidaySchedule.setClassType(ClassScheduleConstants.DayType.HOLIDAY);
            updates.add(holidaySchedule);
            return new ScheduleDeletePlan(
                    targetSchedule.getClassId(),
                    snapshotSchedules(schedules),
                    updates,
                    deleteIds,
                    teacherAssignments);
        }

        // 策略二：普通上课日 → 后续课程依次前移
        // 筛选所有上课类型日程并按日期排序
        List<SysClassSchedule> teachingSchedules = schedules.stream()
                .filter(schedule -> ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType()))
                .sorted(Comparator.comparing(SysClassSchedule::getScheduleDate)
                        .thenComparing(SysClassSchedule::getId))
                .toList();
        // 定位目标日程在上课序列中的索引
        int targetIndex = -1;
        for (int index = 0; index < teachingSchedules.size(); index++) {
            if (Objects.equals(teachingSchedules.get(index).getId(), targetSchedule.getId())) {
                targetIndex = index;
                break;
            }
        }
        if (targetIndex < 0) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }

        // 将目标位置之后的每个上课日程内容前移一位（后一天的课程覆盖到前一天）
        for (int index = targetIndex; index < teachingSchedules.size() - 1; index++) {
            SysClassSchedule destination = copySchedule(teachingSchedules.get(index));
            SysClassSchedule source = teachingSchedules.get(index + 1);
            copyCoursePayload(source, destination);
            updates.add(destination);
            // 记录教师在新日期的分配，用于后续冲突校验
            addTeacherAssignment(
                    teacherAssignments,
                    source,
                    destination.getScheduleDate(),
                    destination.getId());
        }

        // 确定需要删除的尾部日程：若只剩一条上课记录则删除全部，否则删除新结束日期之后的记录
        if (teachingSchedules.size() == 1) {
            deleteIds.addAll(schedules.stream().map(SysClassSchedule::getId).toList());
        } else {
            LocalDate newEndDate = teachingSchedules.get(teachingSchedules.size() - 2).getScheduleDate();
            deleteIds.addAll(schedules.stream()
                    .filter(schedule -> schedule.getScheduleDate().isAfter(newEndDate))
                    .map(SysClassSchedule::getId)
                    .toList());
        }
        return new ScheduleDeletePlan(
                targetSchedule.getClassId(),
                snapshotSchedules(schedules),
                updates,
                deleteIds,
                teacherAssignments);
    }

    /**
     * 在事务中执行删除计划（加锁阶段）。
     * <p>
     * 加锁顺序：班级行 → 教师行（按ID排序） → 课表行（按ID排序），避免死锁。
     * 加锁后通过快照比对确认课表未被并发修改，再执行更新和删除。
     */
    private void executeScheduleDeletePlan(ScheduleDeletePlan plan) {
        // 锁定班级行
        if (sysClassMapper.selectByIdForUpdate(plan.classId()) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        // 按ID排序锁定涉及的教师行，防止死锁
        plan.teacherAssignments().stream()
                .map(TeacherDateAssignment::teacherId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(sysUserMapper::selectByIdForUpdate);
        // 按ID排序锁定涉及的课表行，防止死锁
        plan.expectedSchedules().stream()
                .map(ScheduleSnapshot::id)
                .filter(Objects::nonNull)
                .sorted()
                .forEach(baseMapper::selectByIdForUpdate);

        // 乐观快照比对：确认课表在加锁前未被其他事务修改
        List<SysClassSchedule> currentSchedules = listClassSchedules(plan.classId());
        if (!Objects.equals(plan.expectedSchedules(), snapshotSchedules(currentSchedules))) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课表已发生变化，请刷新后重新删除");
        }
        // 校验前移后教师在新日期是否存在时间冲突
        validatePlannedTeacherAssignments(plan.teacherAssignments());
        // 执行批量更新（课程前移）
        if (!plan.updates().isEmpty() && !this.updateBatchById(plan.updates())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("后续课程前移失败");
        }
        // 执行批量删除（尾部多余日程）
        if (!plan.deleteIds().isEmpty() && !this.removeByIds(plan.deleteIds())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程日程删除失败");
        }
    }

    /**
     * 查询教师分配选项列表（供前端下拉选择）。
     * <p>
     * 返回内容包括：
     * <ul>
     *     <li>课程阶段列表（含日期范围、当前分配状态）</li>
     *     <li>可用讲师列表（含已被其他班级占用的日期）</li>
     * </ul>
     *
     * @param classId 班级ID
     * @return 教师分配选项（阶段 + 讲师）
     */
    @Override
    public SysClassScheduleTeacherAssignmentOptionsRes listTeacherAssignmentOptions(Long classId) {
        requireClass(classId);
        List<SysClassSchedule> classSchedules = listClassSchedules(classId);
        if (classSchedules.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级尚未生成课程表");
        }

        // 筛选有效上课日程，按课程阶段分组
        List<SysClassSchedule> teachingSchedules = classSchedules.stream()
                .filter(this::isTeachingSchedule)
                .toList();
        Map<Long, SysCourseDetail> courseDetailMap = loadCourseDetailMap(teachingSchedules);
        Map<String, List<SysClassSchedule>> stageScheduleMap = groupSchedulesByStage(teachingSchedules, courseDetailMap);
        if (stageScheduleMap.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级课程表暂无可分配教师的课程阶段");
        }

        // 查询所有在职讲师，并计算他们在当前班级上课日期中被其他班级占用的日期
        List<SysUser> lecturers = listActiveLecturers();
        Set<LocalDate> currentClassDates = teachingSchedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .collect(Collectors.toSet());
        Map<Long, List<LocalDate>> occupiedDateMap = listOccupiedDates(
                lecturers.stream().map(SysUser::getId).toList(),
                currentClassDates,
                classId);

        // 加载已分配教师信息，用于展示阶段当前分配状态
        Set<Long> assignedTeacherIds = teachingSchedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> assignedTeacherMap = loadUserMap(assignedTeacherIds);

        // 组装阶段选项和教师选项
        List<SysClassScheduleTeacherAssignmentOptionsRes.StageOption> stages = stageScheduleMap.entrySet().stream()
                .map(entry -> toStageOption(entry.getKey(), entry.getValue(), assignedTeacherMap))
                .toList();
        List<SysClassScheduleTeacherAssignmentOptionsRes.TeacherOption> teachers = lecturers.stream()
                .map(teacher -> new SysClassScheduleTeacherAssignmentOptionsRes.TeacherOption(
                        teacher.getId(),
                        getUserDisplayName(teacher),
                        occupiedDateMap.getOrDefault(teacher.getId(), List.of())))
                .toList();
        return new SysClassScheduleTeacherAssignmentOptionsRes(stages, teachers);
    }

    /**
     * 按课程阶段批量分配授课教师。
     * <p>
     * 将指定阶段下所有上课日程的教师统一设置为同一位讲师。
     * 加锁顺序：班级行 → 教师行，校验教师在这些日期是否已被其他班级占用。
     *
     * @param req 包含班级ID、阶段名称、教师ID
     * @return 分配结果（含日期范围和课程节数）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysClassScheduleTeacherAssignRes assignTeacherByStage(SysClassScheduleTeacherAssignReq req) {
        // 先锁班级，避免同一班级的教师分配、临时加课等写操作并发修改课表。
        SysClass sysClass = sysClassMapper.selectByIdForUpdate(req.classId());
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        // 查询该阶段下的所有上课日程
        String stageName = req.stageName().trim();
        List<SysClassSchedule> stageSchedules = listStageSchedules(sysClass, stageName);

        // 锁定教师行，使同一教师的并发分配串行执行。
        SysUser teacher = sysUserMapper.selectByIdForUpdate(req.teacherId());
        validateLecturer(teacher);

        // 校验教师在这些日期是否已被其他班级占用（排除当前阶段自身日程）
        List<LocalDate> classDates = stageSchedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .toList();
        List<Long> scheduleIds = stageSchedules.stream()
                .map(SysClassSchedule::getId)
                .toList();
        validateTeacherAvailability(teacher.getId(), classDates, scheduleIds);

        // 批量更新该阶段所有日程的教师ID
        boolean updated = this.update(Wrappers.<SysClassSchedule>lambdaUpdate()
                .in(SysClassSchedule::getId, scheduleIds)
                .set(SysClassSchedule::getTeacherId, teacher.getId()));
        if (!updated) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("教师分配失败");
        }

        return new SysClassScheduleTeacherAssignRes(
                sysClass.getId(),
                stageName,
                teacher.getId(),
                getUserDisplayName(teacher),
                classDates.get(0),
                classDates.get(classDates.size() - 1),
                classDates.size());
    }

    /**
     * 查询临时加课选项列表（供前端下拉选择）。
     * <p>
     * 返回可选的课程阶段（含课程节数）和可选的在职讲师列表。
     *
     * @param classId 班级ID
     * @return 临时加课选项（阶段 + 讲师）
     */
    @Override
    public SysClassScheduleTemporaryCourseOptionsRes listTemporaryCourseOptions(Long classId) {
        SysClass sysClass = requireClass(classId);
        // 查询班级关联课程的所有课程详情，按阶段分组统计
        List<SysCourseDetail> courseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId));
        List<SysClassScheduleTemporaryCourseOptionsRes.StageOption> stageOptions = courseDetails.stream()
                .filter(detail -> StrUtil.isNotBlank(detail.getStageName()))
                .collect(Collectors.groupingBy(
                        SysCourseDetail::getStageName,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new SysClassScheduleTemporaryCourseOptionsRes.StageOption(
                        entry.getKey(),
                        entry.getKey() + "（" + entry.getValue().size() + "节课程）",
                        entry.getValue().size()))
                .toList();
        List<SysClassScheduleTemporaryCourseOptionsRes.TeacherOption> teacherOptions = listActiveLecturers().stream()
                .map(teacher -> new SysClassScheduleTemporaryCourseOptionsRes.TeacherOption(
                        teacher.getId(),
                        getUserDisplayName(teacher)))
                .toList();
        return new SysClassScheduleTemporaryCourseOptionsRes(stageOptions, teacherOptions);
    }

    /**
     * 在指定日期增加临时课程。
     * <p>
     * 加课策略：
     * <ul>
     *     <li>若目标日期无日程或为非上课日（自习/休息/节假日），直接在该日期插入或覆盖为临时课程</li>
     *     <li>若目标日期已有上课日程，则将临时课程插入该位置，
     *         原有课程及后续所有上课日程依次向后顺延一位，
     *         并在课表尾部追加新的上课日（跳过非上课日）</li>
     * </ul>
     * 实际写操作在事务中通过加锁 + 快照比对保证并发安全。
     *
     * @param req 包含班级ID、目标日期、课程阶段、教师ID
     * @return 临时加课结果（含是否顺延、顺延节数、新结束日期）
     */
    @Override
    public SysClassScheduleTemporaryCourseRes addTemporaryCourse(SysClassScheduleTemporaryCourseReq req) {
        SysClass sysClass = requireClass(req.classId());
        String stageName = req.stageName().trim();
        // 查找阶段锚点（该阶段的第一条课程详情）
        SysCourseDetail courseDetail = findStageAnchor(sysClass.getCourseId(), stageName);
        SysUser teacher = sysUserMapper.selectById(req.teacherId());
        validateLecturer(teacher);

        List<SysClassSchedule> schedules = listClassSchedules(sysClass.getId());
        if (schedules.isEmpty()) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("该班级尚未生成课程表，无法临时加课");
        }

        // 构建加课计划（无锁阶段，仅计算需要更新/插入的记录）
        TemporaryCoursePlan plan = buildTemporaryCoursePlan(
                sysClass,
                courseDetail,
                teacher,
                req.scheduleDate(),
                schedules);
        // 在事务中加锁执行加课计划
        SysClassScheduleTemporaryCourseRes result = transactionTemplate.execute(
                status -> executeTemporaryCoursePlan(req, plan));
        if (result == null) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时加课失败");
        }
        return result;
    }

    /**
     * 构建临时加课计划（无锁阶段）。
     * <p>
     * 根据目标日期是否已有上课日程，分两种策略：
     * <ul>
     *     <li>无需顺延：目标日期无日程则直接插入，有非上课日程则覆盖</li>
     *     <li>需要顺延：目标日期已有上课日程，将临时课程占据该位置，
     *         原有课程依次后移一位，并在课表尾部扩展新的上课日</li>
     * </ul>
     *
     * @param sysClass     班级实体
     * @param courseDetail 临时课程关联的课程详情（阶段锚点）
     * @param teacher      授课教师
     * @param scheduleDate 目标加课日期
     * @param schedules    该班级的全部课表（按日期排序）
     * @return 加课计划（包含更新列表、插入列表、教师分配校验列表等）
     */
    private TemporaryCoursePlan buildTemporaryCoursePlan(
            SysClass sysClass,
            SysCourseDetail courseDetail,
            SysUser teacher,
            LocalDate scheduleDate,
            List<SysClassSchedule> schedules) {
        // 构建日期 -> 日程映射，便于快速查找目标日期是否已有日程
        Map<LocalDate, SysClassSchedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(
                        SysClassSchedule::getScheduleDate,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        SysClassSchedule targetSchedule = scheduleMap.get(scheduleDate);
        List<SysClassSchedule> updates = new ArrayList<>();
        List<SysClassSchedule> inserts = new ArrayList<>();
        List<TeacherDateAssignment> teacherAssignments = new ArrayList<>();
        // 记录临时课程教师在目标日期的分配（排除目标日程自身，避免自冲突）
        teacherAssignments.add(new TeacherDateAssignment(
                teacher.getId(),
                scheduleDate,
                targetSchedule == null ? null : targetSchedule.getId()));

        // 判断是否需要顺延：目标日期已有上课类型日程时才需要
        boolean shifted = targetSchedule != null
                && ClassScheduleConstants.DayType.CLASS.equals(targetSchedule.getClassType());
        int shiftedClassCount = 0;

        if (!shifted) {
            // 无需顺延：直接插入新日程或覆盖已有非上课日程
            if (targetSchedule == null) {
                inserts.add(createTemporaryCourseSchedule(
                        sysClass.getId(), scheduleDate, courseDetail, teacher.getId()));
            } else {
                SysClassSchedule targetUpdate = copySchedule(targetSchedule);
                fillTemporaryCourse(targetUpdate, courseDetail, teacher.getId());
                updates.add(targetUpdate);
            }
        } else {
            // 需要顺延：筛选目标日期及之后的所有上课日程
            List<SysClassSchedule> shiftedSchedules = schedules.stream()
                    .filter(schedule -> ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType()))
                    .filter(schedule -> !schedule.getScheduleDate().isBefore(scheduleDate))
                    .sorted(Comparator.comparing(SysClassSchedule::getScheduleDate)
                            .thenComparing(SysClassSchedule::getId))
                    .toList();
            shiftedClassCount = shiftedSchedules.size();

            // 第一个位置放置临时课程
            SysClassSchedule targetUpdate = copySchedule(shiftedSchedules.get(0));
            fillTemporaryCourse(targetUpdate, courseDetail, teacher.getId());
            updates.add(targetUpdate);

            // 后续每个位置继承前一个位置的课程内容（依次后移一位）
            for (int index = 1; index < shiftedSchedules.size(); index++) {
                SysClassSchedule source = shiftedSchedules.get(index - 1);
                SysClassSchedule destination = copySchedule(shiftedSchedules.get(index));
                copyCoursePayload(source, destination);
                updates.add(destination);
                addTeacherAssignment(teacherAssignments, source, destination.getScheduleDate(), destination.getId());
            }

            // 在课表尾部扩展：找到下一个可用的上课日，插入被挤出的最后一节课
            SysClassSchedule lastShiftedSchedule = shiftedSchedules.get(shiftedSchedules.size() - 1);
            ScheduleExtension extension = buildScheduleExtension(
                    sysClass,
                    lastShiftedSchedule.getScheduleDate(),
                    scheduleMap,
                    loadRule());
            inserts.addAll(extension.nonClassSchedules());
            SysClassSchedule finalSchedule = createSchedule(
                    sysClass,
                    extension.nextClassDate(),
                    null,
                    lastShiftedSchedule.getCourseContent(),
                    ClassScheduleConstants.DayType.CLASS);
            copyCoursePayload(lastShiftedSchedule, finalSchedule);
            inserts.add(finalSchedule);
            addTeacherAssignment(
                    teacherAssignments,
                    lastShiftedSchedule,
                    finalSchedule.getScheduleDate(),
                    null);
        }

        // 计算加课后的新课表结束日期
        LocalDate endDate = schedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(scheduleDate);
        for (SysClassSchedule insert : inserts) {
            if (insert.getScheduleDate().isAfter(endDate)) {
                endDate = insert.getScheduleDate();
            }
        }

        return new TemporaryCoursePlan(
                sysClass,
                courseDetail,
                scheduleDate,
                snapshotSchedules(schedules),
                updates,
                inserts,
                teacherAssignments,
                shifted,
                shiftedClassCount,
                endDate);
    }

    /**
     * 在事务中执行临时加课计划（加锁阶段）。
     * <p>
     * 加锁顺序：班级行 → 教师行（按ID排序），避免死锁。
     * 加锁后通过快照比对确认课表未被并发修改，再执行更新和插入。
     */
    private SysClassScheduleTemporaryCourseRes executeTemporaryCoursePlan(
            SysClassScheduleTemporaryCourseReq req,
            TemporaryCoursePlan plan) {
        // 锁定班级行
        if (sysClassMapper.selectByIdForUpdate(req.classId()) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        // 按ID排序锁定涉及的教师行，防止死锁
        Map<Long, SysUser> lockedTeacherMap = new LinkedHashMap<>();
        plan.teacherAssignments().stream()
                .map(TeacherDateAssignment::teacherId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(teacherId -> lockedTeacherMap.put(
                        teacherId,
                        sysUserMapper.selectByIdForUpdate(teacherId)));
        SysUser temporaryTeacher = lockedTeacherMap.get(req.teacherId());
        validateLecturer(temporaryTeacher);

        // 乐观快照比对：确认课表在加锁前未被其他事务修改
        List<SysClassSchedule> currentSchedules = listClassSchedules(req.classId());
        if (!Objects.equals(plan.expectedSchedules(), snapshotSchedules(currentSchedules))) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课表已发生变化，请刷新后重新加课");
        }

        // 校验所有涉及的教师在新日期是否存在时间冲突
        validatePlannedTeacherAssignments(plan.teacherAssignments());
        // 执行批量更新（课程顺延）
        if (!plan.updates().isEmpty() && !this.updateBatchById(plan.updates())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时课程调整失败");
        }
        // 执行批量插入（新增日程）
        if (!plan.inserts().isEmpty() && !this.saveBatch(plan.inserts())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时课程保存失败");
        }

        return new SysClassScheduleTemporaryCourseRes(
                plan.sysClass().getId(),
                plan.scheduleDate(),
                plan.courseDetail().getStageName(),
                TEMPORARY_COURSE_CONTENT,
                temporaryTeacher.getId(),
                getUserDisplayName(temporaryTeacher),
                plan.shifted(),
                plan.shiftedClassCount(),
                plan.endDate());
    }

    /**
     * 批量校验计划中所有教师在新日期的可用性。
     * <p>
     * 按教师ID分组后逐一校验，确保每位教师在对应日期未被其他班级占用。
     */
    private void validatePlannedTeacherAssignments(List<TeacherDateAssignment> assignments) {
        // 按教师ID分组
        Map<Long, List<TeacherDateAssignment>> assignmentMap = assignments.stream()
                .filter(assignment -> assignment.teacherId() != null)
                .collect(Collectors.groupingBy(TeacherDateAssignment::teacherId));
        // 按教师ID排序后逐一校验（保证加锁顺序一致，避免死锁）
        assignmentMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> validateTeacherAvailability(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(TeacherDateAssignment::scheduleDate)
                                .distinct()
                                .toList(),
                        entry.getValue().stream()
                                .map(TeacherDateAssignment::excludedScheduleId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()));
    }

    /**
     * 校验教师在指定日期集合中是否已被其他班级占用。
     * <p>
     * 查询该教师在目标日期是否已有上课类型日程（排除 excludedScheduleIds 中的日程），
     * 若存在冲突则抛出异常并提示冲突班级信息。
     *
     * @param teacherId          教师ID
     * @param classDates         需要校验的日期集合
     * @param excludedScheduleIds 需要排除的日程ID（即当前正在操作的日程，不算冲突）
     */
    private void validateTeacherAvailability(
            Long teacherId,
            Collection<LocalDate> classDates,
            Collection<Long> excludedScheduleIds) {
        if (teacherId == null || classDates == null || classDates.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("教师ID和上课日期不能为空");
        }
        // 查询该教师在目标日期是否已有其他班级的上课日程（排除自身日程）
        List<Long> excludedIds = excludedScheduleIds == null
                ? List.of()
                : excludedScheduleIds.stream().filter(Objects::nonNull).distinct().toList();
        SysClassSchedule conflict = this.getOne(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getTeacherId, teacherId)
                .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                .in(SysClassSchedule::getScheduleDate, classDates)
                .notIn(!excludedIds.isEmpty(), SysClassSchedule::getId, excludedIds)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .last("LIMIT 1"), false);
        if (conflict == null) {
            return;
        }
        SysClass conflictClass = sysClassMapper.selectById(conflict.getClassId());
        String className = conflictClass == null ? "其他班级" : "班级“" + conflictClass.getClassPeriod() + "”";
        throw BusinessException.TEACHER_SCHEDULE_CONFLICT.newInstance(
                "该教师在" + conflict.getScheduleDate() + "已为" + className + "授课，请选择其他教师");
    }

    /** 根据ID查询班级，不存在则抛出异常 */
    private SysClass requireClass(Long classId) {
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        return sysClass;
    }

    /** 根据ID查询课程日程，不存在则抛出异常 */
    private SysClassSchedule requireSchedule(Long scheduleId) {
        SysClassSchedule schedule = baseMapper.selectById(scheduleId);
        if (schedule == null) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }
        return schedule;
    }

    /** 校验日程必须为上课类型，否则抛出异常（自习/休息/节假日不可修改或删除） */
    private void requireTeachingSchedule(SysClassSchedule schedule) {
        if (!ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("只有上课日程可以修改或删除");
        }
    }

    /**
     * 查找指定课程阶段的锚点记录（该阶段的第一条课程详情）。
     * 用于临时加课或修改日程时关联课程详情。
     */
    private SysCourseDetail findStageAnchor(Long courseId, String stageName) {
        List<SysCourseDetail> stageCourseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, courseId)
                        .eq(SysCourseDetail::getStageName, stageName)
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId));
        if (stageCourseDetails.isEmpty()) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("所选课程阶段不存在");
        }
        return stageCourseDetails.get(0);
    }

    /** 将单条课程日程转换为响应对象（关联阶段名称和教师显示名） */
    private SysClassScheduleRes toScheduleResponse(SysClassSchedule schedule) {
        SysCourseDetail detail = schedule.getCourseDetailId() == null
                ? null
                : sysCourseDetailMapper.selectById(schedule.getCourseDetailId());
        SysUser teacher = schedule.getTeacherId() == null
                ? null
                : sysUserMapper.selectById(schedule.getTeacherId());
        return SysClassScheduleRes.from(
                schedule,
                detail == null ? null : detail.getStageName(),
                teacher == null ? null : getUserDisplayName(teacher));
    }

    /** 查询指定班级的全部课表，按日期和ID升序排列 */
    private List<SysClassSchedule> listClassSchedules(Long classId) {
        return this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .orderByAsc(SysClassSchedule::getId));
    }

    /** 判断日程是否为有效上课日程（上课类型且关联了课程详情） */
    private boolean isTeachingSchedule(SysClassSchedule schedule) {
        return ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType())
                && schedule.getCourseDetailId() != null;
    }

    /** 批量加载课程详情，构建 ID -> 课程详情 映射 */
    private Map<Long, SysCourseDetail> loadCourseDetailMap(List<SysClassSchedule> schedules) {
        List<Long> detailIds = schedules.stream()
                .map(SysClassSchedule::getCourseDetailId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (detailIds.isEmpty()) {
            return Map.of();
        }
        return sysCourseDetailMapper.selectBatchIds(detailIds).stream()
                .collect(Collectors.toMap(SysCourseDetail::getId, Function.identity()));
    }

    /** 将上课日程按课程阶段名称分组（保持插入顺序） */
    private Map<String, List<SysClassSchedule>> groupSchedulesByStage(
            List<SysClassSchedule> schedules,
            Map<Long, SysCourseDetail> courseDetailMap) {
        return schedules.stream()
                .filter(schedule -> {
                    SysCourseDetail detail = courseDetailMap.get(schedule.getCourseDetailId());
                    return detail != null && StrUtil.isNotBlank(detail.getStageName());
                })
                .collect(Collectors.groupingBy(
                        schedule -> courseDetailMap.get(schedule.getCourseDetailId()).getStageName(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /** 查询所有在职（ACTIVE）且具有讲师角色的用户列表 */
    private List<SysUser> listActiveLecturers() {
        SysRole lecturerRole = findLecturerRole();
        List<Long> lecturerIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery()
                                .eq(SysUserRole::getRoleId, lecturerRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (lecturerIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, lecturerIds)
                .eq(SysUser::getStatus, ActiveEnum.ACTIVE.name())
                .orderByAsc(SysUser::getId));
    }

    /**
     * 查询讲师们在当前班级上课日期中被其他班级占用的日期。
     * 用于前端展示教师时间冲突提示。
     */
    private Map<Long, List<LocalDate>> listOccupiedDates(
            List<Long> lecturerIds,
            Set<LocalDate> currentClassDates,
            Long currentClassId) {
        if (lecturerIds.isEmpty() || currentClassDates.isEmpty()) {
            return Map.of();
        }
        return this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                        .in(SysClassSchedule::getTeacherId, lecturerIds)
                        .in(SysClassSchedule::getScheduleDate, currentClassDates)
                        .ne(SysClassSchedule::getClassId, currentClassId)
                        .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                        .orderByAsc(SysClassSchedule::getScheduleDate))
                .stream()
                .collect(Collectors.groupingBy(
                        SysClassSchedule::getTeacherId,
                        Collectors.mapping(
                                SysClassSchedule::getScheduleDate,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(HashSet::new),
                                        dates -> dates.stream().sorted().toList()))));
    }

    /**
     * 将某个阶段的日程列表转换为前端阶段选项。
     * 包含日期范围、当前分配的教师信息（未分配/分配不完整/多位教师/已分配）。
     */
    private SysClassScheduleTeacherAssignmentOptionsRes.StageOption toStageOption(
            String stageName,
            List<SysClassSchedule> schedules,
            Map<Long, SysUser> teacherMap) {
        List<LocalDate> classDates = schedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .distinct()
                .sorted()
                .toList();
        List<Long> teacherIds = schedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        boolean hasUnassignedSchedule = schedules.stream()
                .anyMatch(schedule -> schedule.getTeacherId() == null);
        Long teacherId = teacherIds.size() == 1 && !hasUnassignedSchedule ? teacherIds.get(0) : null;
        String teacherName = null;
        if (hasUnassignedSchedule && !teacherIds.isEmpty()) {
            teacherName = "分配不完整";
        } else if (teacherIds.size() > 1) {
            teacherName = "多位教师";
        } else if (teacherId != null) {
            SysUser teacher = teacherMap.get(teacherId);
            teacherName = teacher == null ? "教师ID：" + teacherId : getUserDisplayName(teacher);
        }
        return new SysClassScheduleTeacherAssignmentOptionsRes.StageOption(
                stageName,
                classDates,
                teacherId,
                teacherName);
    }

    /**
     * 查询指定班级中某个课程阶段的所有上课日程。
     * 先查找该阶段对应的课程详情ID列表，再筛选课表中关联这些详情的上课日程。
     */
    private List<SysClassSchedule> listStageSchedules(SysClass sysClass, String stageName) {
        List<Long> courseDetailIds = sysCourseDetailMapper.selectList(
                        Wrappers.<SysCourseDetail>lambdaQuery()
                                .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                                .eq(SysCourseDetail::getStageName, stageName)
                                .orderByAsc(SysCourseDetail::getDayNumber)
                                .orderByAsc(SysCourseDetail::getId))
                .stream()
                .map(SysCourseDetail::getId)
                .toList();
        if (courseDetailIds.isEmpty()) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("所选课程阶段不存在");
        }
        List<SysClassSchedule> schedules = this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, sysClass.getId())
                .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                .in(SysClassSchedule::getCourseDetailId, courseDetailIds)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .orderByAsc(SysClassSchedule::getId));
        if (schedules.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("所选阶段在当前班级课表中没有上课记录");
        }
        return schedules;
    }

    /** 校验用户是否为合法讲师：存在、在职、且具有 LECTURER 角色 */
    private void validateLecturer(SysUser teacher) {
        if (teacher == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("所选授课教师不存在");
        }
        if (!ActiveEnum.ACTIVE.name().equals(teacher.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("所选授课教师已停用");
        }
        SysRole lecturerRole = findLecturerRole();
        boolean isLecturer = sysUserRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, teacher.getId())
                        .eq(SysUserRole::getRoleId, lecturerRole.getId())) > 0;
        if (!isLecturer) {
            throw BusinessException.DATA_ERROR.newInstance("所选用户不是讲师角色");
        }
    }

    /** 查找讲师角色（LECTURER），不存在则抛出异常 */
    private SysRole findLecturerRole() {
        SysRole lecturerRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, LECTURER_ROLE_CODE));
        if (lecturerRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("讲师角色LECTURER不存在");
        }
        return lecturerRole;
    }

    /** 批量加载用户信息，构建 ID -> 用户 映射 */
    private Map<Long, SysUser> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    /**
     * 获取用户显示名称。
     * 优先级：真实姓名 > 昵称 > 用户名；若显示名与用户名不同则追加用户名后缀。
     */
    private String getUserDisplayName(SysUser user) {
        String name = StrUtil.isNotBlank(user.getRealName())
                ? user.getRealName()
                : StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        if (StrUtil.isNotBlank(user.getUsername()) && !Objects.equals(name, user.getUsername())) {
            return name + "（" + user.getUsername() + "）";
        }
        return name;
    }

    /** 创建一条临时课程日程（用于目标日期无已有日程时直接插入） */
    private SysClassSchedule createTemporaryCourseSchedule(
            Long classId,
            LocalDate scheduleDate,
            SysCourseDetail courseDetail,
            Long teacherId) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setClassId(classId);
        schedule.setScheduleDate(scheduleDate);
        fillTemporaryCourse(schedule, courseDetail, teacherId);
        return schedule;
    }

    /** 将日程填充为临时课程（设置教师、课程详情、内容和类型） */
    private void fillTemporaryCourse(
            SysClassSchedule schedule,
            SysCourseDetail courseDetail,
            Long teacherId) {
        schedule.setTeacherId(teacherId);
        schedule.setCourseDetailId(courseDetail.getId());
        schedule.setCourseContent(TEMPORARY_COURSE_CONTENT);
        schedule.setClassType(ClassScheduleConstants.DayType.CLASS);
    }

    /** 浅拷贝日程对象（保留ID，用于构建更新计划） */
    private SysClassSchedule copySchedule(SysClassSchedule source) {
        SysClassSchedule target = new SysClassSchedule();
        target.setId(source.getId());
        target.setClassId(source.getClassId());
        target.setTeacherId(source.getTeacherId());
        target.setScheduleDate(source.getScheduleDate());
        target.setCourseDetailId(source.getCourseDetailId());
        target.setCourseContent(source.getCourseContent());
        target.setClassType(source.getClassType());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    /** 将源日程的课程内容（教师、课程详情、内容、类型）复制到目标日程，用于课程前移/顺延 */
    private void copyCoursePayload(SysClassSchedule source, SysClassSchedule destination) {
        destination.setTeacherId(source.getTeacherId());
        destination.setCourseDetailId(source.getCourseDetailId());
        destination.setCourseContent(source.getCourseContent());
        destination.setClassType(ClassScheduleConstants.DayType.CLASS);
    }

    /** 若源日程有教师，则记录该教师在新日期的分配（用于后续冲突校验） */
    private void addTeacherAssignment(
            List<TeacherDateAssignment> assignments,
            SysClassSchedule source,
            LocalDate destinationDate,
            Long excludedScheduleId) {
        if (source.getTeacherId() != null) {
            assignments.add(new TeacherDateAssignment(
                    source.getTeacherId(),
                    destinationDate,
                    excludedScheduleId));
        }
    }

    /**
     * 从最后一个上课日的次日开始，按排课规则向后扩展课表，
     * 直到找到下一个可用的上课日。中间遇到的非上课日（自习/休息/节假日）作为新增日程返回。
     *
     * @return 扩展结果（含中间非上课日程列表和下一个上课日期）
     */
    private ScheduleExtension buildScheduleExtension(
            SysClass sysClass,
            LocalDate lastClassDate,
            Map<LocalDate, SysClassSchedule> existingScheduleMap,
            ScheduleRule rule) {
        List<SysClassSchedule> extensionSchedules = new ArrayList<>();
        LocalDate currentDate = lastClassDate.plusDays(1);

        for (int days = 0; days < MAX_GENERATION_DAYS; days++) {
            // 跳过已有日程的日期
            if (existingScheduleMap.containsKey(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }
            // 解析当前日期的日程类型
            ResolvedScheduleDay resolvedDay = resolveScheduleDay(currentDate, rule);
            // 找到上课日则停止扩展
            if (ClassScheduleConstants.DayType.CLASS.equals(resolvedDay.classType())) {
                return new ScheduleExtension(extensionSchedules, currentDate);
            }
            // 非上课日加入扩展列表
            extensionSchedules.add(createSchedule(
                    sysClass,
                    currentDate,
                    null,
                    resolvedDay.courseContent(),
                    resolvedDay.classType()));
            currentDate = currentDate.plusDays(1);
        }
        throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课表顺延跨度过大，请检查排课规则");
    }

    /**
     * 根据排课规则解析指定日期的日程类型和内容。
     * 优先级：节假日 > 上课日 > 自习日 > 休息日。
     */
    private ResolvedScheduleDay resolveScheduleDay(LocalDate date, ScheduleRule rule) {
        HolidayInfo holidayInfo = getHolidayInfo(date);
        int weekDay = date.getDayOfWeek().getValue();
        if (rule.holidayRest() && isHoliday(holidayInfo)) {
            String holidayName = StrUtil.isNotBlank(holidayInfo.getName())
                    ? holidayInfo.getName()
                    : "法定节假日";
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.HOLIDAY, holidayName);
        }
        if (rule.classDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.CLASS, null);
        }
        if (rule.selfStudyDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.SELF_STUDY, "自习");
        }
        if (rule.restDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.REST, "休息");
        }
        throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("当前日期未匹配到排课规则");
    }

    /** 将课表列表转换为不可变快照，用于并发安全比对（乐观锁） */
    private List<ScheduleSnapshot> snapshotSchedules(List<SysClassSchedule> schedules) {
        return schedules.stream()
                .map(schedule -> new ScheduleSnapshot(
                        schedule.getId(),
                        schedule.getClassId(),
                        schedule.getTeacherId(),
                        schedule.getScheduleDate(),
                        schedule.getCourseDetailId(),
                        schedule.getCourseContent(),
                        schedule.getClassType()))
                .toList();
    }

    /** 根据班级ID删除该班级的全部课表（用于班级删除时级联清理） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByClassId(Long classId) {
        this.remove(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId));
    }

    /**
     * 核心排课算法：从起始日期开始逐日生成课表，直到所有课程详情消耗完毕。
     * <p>
     * 每天根据排课规则判断日程类型：
     * <ul>
     *     <li>节假日（若开启节假日休息）→ HOLIDAY</li>
     *     <li>上课日 → CLASS，消耗一条课程详情</li>
     *     <li>自习日 → SELF_STUDY</li>
     *     <li>休息日 → REST</li>
     * </ul>
     */
    private List<SysClassSchedule> buildSchedules(
            SysClass sysClass,
            LocalDate startDate,
            List<SysCourseDetail> courseDetails,
            ScheduleRule rule) {
        List<SysClassSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = startDate;
        int detailIndex = 0;

        while (detailIndex < courseDetails.size()) {
            // 防御性检查：防止排课规则配置异常导致无限循环
            if (schedules.size() >= MAX_GENERATION_DAYS) {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课跨度过大，请检查排课规则");
            }

            HolidayInfo holidayInfo = getHolidayInfo(currentDate);
            int weekDay = currentDate.getDayOfWeek().getValue();
            SysCourseDetail courseDetail = null;
            String classType;
            String courseContent;

            // 按优先级判断当天日程类型
            if (rule.holidayRest() && isHoliday(holidayInfo)) {
                classType = ClassScheduleConstants.DayType.HOLIDAY;
                courseContent = StrUtil.isNotBlank(holidayInfo.getName()) ? holidayInfo.getName() : "法定节假日";
            } else if (rule.classDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.CLASS;
                courseDetail = courseDetails.get(detailIndex++);
                courseContent = getCourseContent(courseDetail);
            } else if (rule.selfStudyDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.SELF_STUDY;
                courseContent = "自习";
            } else if (rule.restDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.REST;
                courseContent = "休息";
            } else {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("当前日期未匹配到排课规则");
            }

            schedules.add(createSchedule(sysClass, currentDate, courseDetail, courseContent, classType));
            currentDate = currentDate.plusDays(1);
        }
        return schedules;
    }

    /** 创建单条课程日程实体 */
    private SysClassSchedule createSchedule(
            SysClass sysClass,
            LocalDate scheduleDate,
            SysCourseDetail courseDetail,
            String courseContent,
            String classType) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setClassId(sysClass.getId());
        schedule.setScheduleDate(scheduleDate);
        schedule.setCourseDetailId(courseDetail == null ? null : courseDetail.getId());
        schedule.setCourseContent(courseContent);
        schedule.setClassType(classType);
        return schedule;
    }

    /** 获取课程内容描述：优先使用课程内容 > 阶段名称 > 默认“课程第N天” */
    private String getCourseContent(SysCourseDetail detail) {
        if (StrUtil.isNotBlank(detail.getClassContent())) {
            return detail.getClassContent();
        }
        if (StrUtil.isNotBlank(detail.getStageName())) {
            return detail.getStageName();
        }
        return "课程第" + detail.getDayNumber() + "天";
    }

    /** 从系统配置服务加载全局排课规则，并转换为内部 ScheduleRule 对象 */
    private ScheduleRule loadRule() {
        ClassScheduleRuleRes config = sysConfigService.getClassScheduleRule();
        // 配置服务负责校验星期范围、互斥关系和完整覆盖；此处防御服务契约被未来实现破坏。
        if (config == null || config.classDays() == null
                || config.selfStudyDays() == null || config.restDays() == null) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课规则配置不完整");
        }
        return new ScheduleRule(
                Set.copyOf(config.classDays()),
                Set.copyOf(config.selfStudyDays()),
                Set.copyOf(config.restDays()),
                config.holidayRest());
    }

    /** 获取指定日期的节假日信息 */
    private HolidayInfo getHolidayInfo(LocalDate date) {
        return holidayUtil.getHolidayInfo(date);
    }

    /** 判断节假日信息是否表示法定节假日 */
    private boolean isHoliday(HolidayInfo holidayInfo) {
        return holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday());
    }

    // ==================== 内部记录类型（用于封装计划/快照/规则等中间数据） ====================

    /** 临时加课执行计划：包含需要更新/插入的日程、教师分配校验列表、顺延信息等 */
    private record TemporaryCoursePlan(
            SysClass sysClass,
            SysCourseDetail courseDetail,
            LocalDate scheduleDate,
            List<ScheduleSnapshot> expectedSchedules,
            List<SysClassSchedule> updates,
            List<SysClassSchedule> inserts,
            List<TeacherDateAssignment> teacherAssignments,
            boolean shifted,
            int shiftedClassCount,
            LocalDate endDate) {
    }

    /** 课程日程删除执行计划：包含需要更新/删除的日程、教师分配校验列表 */
    private record ScheduleDeletePlan(
            Long classId,
            List<ScheduleSnapshot> expectedSchedules,
            List<SysClassSchedule> updates,
            List<Long> deleteIds,
            List<TeacherDateAssignment> teacherAssignments) {
    }

    /** 教师-日期分配记录：用于校验教师在新日期是否可用（排除自身日程） */
    private record TeacherDateAssignment(
            Long teacherId,
            LocalDate scheduleDate,
            Long excludedScheduleId) {
    }

    /** 课表快照：用于乐观锁比对，检测课表是否被并发修改 */
    private record ScheduleSnapshot(
            Long id,
            Long classId,
            Long teacherId,
            LocalDate scheduleDate,
            Long courseDetailId,
            String courseContent,
            String classType) {
    }

    /** 课表扩展结果：包含顺延过程中产生的非上课日程和下一个可用上课日期 */
    private record ScheduleExtension(
            List<SysClassSchedule> nonClassSchedules,
            LocalDate nextClassDate) {
    }

    /** 单日日程解析结果：包含日程类型和课程内容 */
    private record ResolvedScheduleDay(
            String classType,
            String courseContent) {
    }

    /** 排课规则：上课日/自习日/休息日的星期集合 + 节假日是否休息 */
    private record ScheduleRule(
            Set<Integer> classDays,
            Set<Integer> selfStudyDays,
            Set<Integer> restDays,
            boolean holidayRest) {
    }
}
