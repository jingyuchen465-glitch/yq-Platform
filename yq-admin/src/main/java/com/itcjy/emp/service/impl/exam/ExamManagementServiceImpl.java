package com.itcjy.emp.service.impl.exam;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.*;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.*;
import com.itcjy.emp.service.exam.IExamManagementService;
import com.itcjy.stu.mapper.StudentMapper;
import com.itcjy.stu.pojo.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考试管理服务实现类
 * <p>提供考试发布、分页查询、考试记录查看、主观题批改及答卷结果公布等管理端功能</p>
 */
@Service
@RequiredArgsConstructor
public class ExamManagementServiceImpl implements IExamManagementService {
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final StudentExamRecordMapper recordMapper;
    private final StudentExamAnswerMapper answerMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;

    /**
     * 发布考试
     * <p>校验试卷、班级、监考老师合法性，为每个班级创建考试并生成学生考试记录，
     * 发布后草稿试卷自动锁定</p>
     *
     * @param req 发布考试请求参数
     * @return 发布成功的考试列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ExamAdminRes> publish(ExamPublishReq req) {
        // 校验最晚入场时间必须晚于开始时间
        if (!req.startTime().isBefore(req.entryDeadlineTime())) {
            throw BusinessException.PARAMS_ERROR.newInstance("最晚入场时间必须晚于开始时间");
        }
        // 校验试卷是否存在且未归档
        ExamPaper paper = requirePaper(req.paperId());
        if (PaperStatus.ARCHIVED.name().equals(paper.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("已归档试卷不能发布");
        }
        // 校验监考老师是否存在
        if (req.invigilatorUserId() != null && userMapper.selectById(req.invigilatorUserId()) == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("监考老师不存在");
        }
        // 校验班级不能重复且必须存在
        List<Long> classIds = req.classIds().stream().distinct().toList();
        if (classIds.size() != req.classIds().size()) throw BusinessException.DATA_EXIST.newInstance("班级不能重复");
        List<SysClass> classes = classMapper.selectBatchIds(classIds);
        if (classes.size() != classIds.size()) throw BusinessException.CLAZZ_NOT_EXIST;
        // 校验班级所属课程与试卷课程一致
        classes.forEach(clazz -> {
            if (!paper.getCourseId().equals(clazz.getCourseId())) {
                throw BusinessException.DATA_ERROR.newInstance("班级课程与试卷课程不一致: " + clazz.getClassPeriod());
            }
        });
        LoginSession session = AuthThreadlocal.getLoginInfo();
        Long operatorId = session == null ? null : session.getPrincipalId();
        // 计算考试关闭时间 = 最晚入场时间 + 考试时长
        LocalDateTime closeTime = req.entryDeadlineTime().plusMinutes(req.durationMinutes());
        List<Long> examIds = new ArrayList<>();
        try {
            // 为每个班级创建考试并生成学生考试记录
            for (Long classId : classIds) {
                Exam exam = new Exam();
                exam.setPaperId(paper.getId()); exam.setClassId(classId); exam.setStartTime(req.startTime());
                exam.setEntryDeadlineTime(req.entryDeadlineTime()); exam.setDurationMinutes(req.durationMinutes());
                exam.setCloseTime(closeTime); exam.setInvigilatorUserId(req.invigilatorUserId());
                exam.setAnswerVisible(false); exam.setCreatedBy(operatorId); examMapper.insert(exam);
                // 查询班级下所有在籍学生，为每人创建考试记录
                List<Student> students = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                        .eq(Student::getClassId, classId).ne(Student::getStatus, Student.WITCHDRAWAL));
                for (Student student : students) {
                    StudentExamRecord record = new StudentExamRecord();
                    record.setExamId(exam.getId()); record.setStudentId(student.getId());
                    record.setStatus(ExamRecordStatus.NOT_STARTED.name());
                    record.setGradingStatus(GradingStatus.PENDING.name());
                    record.setObjectiveScore(BigDecimal.ZERO); record.setSubjectiveScore(BigDecimal.ZERO);
                    record.setVersion(0); recordMapper.insert(record);
                }
                examIds.add(exam.getId());
            }
        } catch (DuplicateKeyException ex) {
            throw BusinessException.DATA_EXIST.newInstance("相同试卷、班级和开始时间的考试已发布");
        }
        // 草稿试卷发布后自动锁定，防止再修改
        if (PaperStatus.DRAFT.name().equals(paper.getStatus())) {
            paper.setStatus(PaperStatus.LOCKED.name()); paperMapper.updateById(paper);
        }
        return examIds.stream().map(this::detail).toList();
    }

    /**
     * 分页查询考试列表
     *
     * @param req 分页查询参数（支持按试卷、班级、开始时间范围筛选）
     * @return 考试分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamAdminRes> page(ExamPageReq req) {
        IPage<Exam> page = examMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<Exam>lambdaQuery()
                        .eq(req.getPaperId() != null, Exam::getPaperId, req.getPaperId())
                        .eq(req.getClassId() != null, Exam::getClassId, req.getClassId())
                        .ge(req.getStartFrom() != null, Exam::getStartTime, req.getStartFrom())
                        .le(req.getStartTo() != null, Exam::getStartTime, req.getStartTo())
                        .orderByDesc(Exam::getStartTime).orderByDesc(Exam::getId));
        return new PageResult<>(page.getTotal(), assemble(page.getRecords()));
    }

    /**
     * 查看考试详情
     *
     * @param id 考试ID
     * @return 考试详情（包含考生统计信息）
     */
    @Override
    @Transactional(readOnly = true)
    public ExamAdminRes detail(Long id) {
        Exam exam = requireExam(id);
        return assemble(List.of(exam)).get(0);
    }

    /**
     * 分页查询某场考试的学生考试记录
     *
     * @param examId 考试ID
     * @param req    分页查询参数（支持按学生姓名、考试状态、批改状态筛选）
     * @return 学生考试记录分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamRecordRes> records(Long examId, ExamRecordPageReq req) {
        requireExam(examId);
        String status = normalizeFilter(req.getStatus());
        String gradingStatus = normalizeFilter(req.getGradingStatus());
        List<Long> studentIds = null;
        if (StrUtil.isNotBlank(req.getStudentKeyword())) {
            studentIds = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                            .select(Student::getId).like(Student::getName, StrUtil.trim(req.getStudentKeyword())))
                    .stream().map(Student::getId).toList();
            if (studentIds.isEmpty()) return new PageResult<>(0L, List.of());
        }
        IPage<StudentExamRecord> page = recordMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<StudentExamRecord>lambdaQuery()
                        .eq(StudentExamRecord::getExamId, examId)
                        .eq(status != null, StudentExamRecord::getStatus, status)
                        .eq(gradingStatus != null, StudentExamRecord::getGradingStatus, gradingStatus)
                        .in(studentIds != null, StudentExamRecord::getStudentId, studentIds)
                        .orderByAsc(StudentExamRecord::getStudentId));
        Map<Long, Student> students = page.getRecords().isEmpty() ? Map.of() :
                studentMapper.selectBatchIds(page.getRecords().stream()
                                .map(StudentExamRecord::getStudentId).toList()).stream()
                        .collect(Collectors.toMap(Student::getId, Function.identity()));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(record -> ExamRecordRes.from(record, students.get(record.getStudentId()))).toList());
    }

    /**
     * 查看主观题批改详情
     * <p>查询指定考试记录中所有填空题和简答题的作答情况</p>
     *
     * @param recordId 考试记录ID
     * @return 批改详情（包含学生信息和主观题答案列表）
     */
    @Override
    @Transactional(readOnly = true)
    public ExamGradingRes gradingDetail(Long recordId) {
        StudentExamRecord record = requireRecord(recordId);
        Student student = studentMapper.selectById(record.getStudentId());
        List<StudentExamAnswer> answers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId)
                .in(StudentExamAnswer::getQuestionType, QuestionType.FILL.name(), QuestionType.SHORT.name())
                .orderByAsc(StudentExamAnswer::getPaperQuestionId));
        List<Long> paperQuestionIds = answers.stream().map(StudentExamAnswer::getPaperQuestionId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ExamPaperQuestion> questions = paperQuestionIds.isEmpty() ? Map.of() :
                paperQuestionMapper.selectBatchIds(paperQuestionIds).stream()
                        .collect(Collectors.toMap(ExamPaperQuestion::getId, Function.identity()));
        return new ExamGradingRes(ExamRecordRes.from(record, student), answers.stream()
                .map(answer -> ExamGradingRes.SubjectiveAnswer.from(answer, questions.get(answer.getPaperQuestionId()))).toList());
    }

    /**
     * 批改主观题
     * <p>对指定考试记录中的主观题进行评分，自动汇总主观题得分并更新批改状态</p>
     *
     * @param recordId 考试记录ID
     * @param req      批改请求（包含每道题的得分和评语）
     * @return 批改后的详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamGradingRes grade(Long recordId, ExamGradeReq req) {
        StudentExamRecord record = requireRecord(recordId);
        // 未提交的考试不能批改
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus()) || ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("考试尚未提交，不能批改");
        }
        // 查询该记录的所有主观题答案
        Map<Long, StudentExamAnswer> answers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                        .eq(StudentExamAnswer::getRecordId, recordId)
                        .in(StudentExamAnswer::getQuestionType, QuestionType.FILL.name(), QuestionType.SHORT.name()))
                .stream().collect(Collectors.toMap(StudentExamAnswer::getId, Function.identity()));
        LoginSession session = AuthThreadlocal.getLoginInfo();
        Long graderId = session == null ? null : session.getPrincipalId();
        LocalDateTime now = LocalDateTime.now();
        // 逐题评分并校验得分不超过题目分值
        for (ExamGradeReq.AnswerGrade grade : req.answers()) {
            StudentExamAnswer answer = answers.get(grade.answerId());
            if (answer == null) throw BusinessException.DATA_ERROR.newInstance("批改答案不属于该考试记录");
            if (grade.score().compareTo(answer.getQuestionScore()) > 0) {
                throw BusinessException.PARAMS_ERROR.newInstance("题目得分不能超过题目分值");
            }
            answer.setScore(grade.score()); answer.setGraderComment(StrUtil.trim(grade.comment()));
            answer.setGraderUserId(graderId); answer.setGradedAt(now); answerMapper.updateById(answer);
        }
        // 重新查询主观题答案，汇总主观题总分
        List<StudentExamAnswer> subjectiveAnswers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId)
                .in(StudentExamAnswer::getQuestionType, QuestionType.FILL.name(), QuestionType.SHORT.name()));
        BigDecimal subjectiveScore = subjectiveAnswers.stream().map(StudentExamAnswer::getScore)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 判断是否所有主观题均已批改完成
        boolean completed = subjectiveAnswers.stream().allMatch(answer -> answer.getScore() != null);
        record.setSubjectiveScore(subjectiveScore);
        record.setScore(record.getObjectiveScore().add(subjectiveScore));
        record.setGradingStatus(completed ? GradingStatus.COMPLETED.name() : GradingStatus.GRADING.name());
        recordMapper.updateById(record);
        return gradingDetail(recordId);
    }

    /**
     * 更新答卷结果可见性
     * <p>考试关闭后才能公布结果，公布后学生可查看自己的答卷</p>
     *
     * @param examId 考试ID
     * @param req    可见性请求（是否公布）
     * @return 更新后的考试详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamAdminRes updateVisibility(Long examId, ExamVisibilityReq req) {
        Exam exam = requireExam(examId);
        // 考试关闭前不允许公布结果
        if (LocalDateTime.now().isBefore(exam.getCloseTime())) {
            throw BusinessException.DATA_ERROR.newInstance("整场考试关闭后才能公布答卷结果");
        }
        exam.setAnswerVisible(req.answerVisible());
        exam.setAnswerVisibleAt(Boolean.TRUE.equals(req.answerVisible()) ? LocalDateTime.now() : null);
        examMapper.updateById(exam);
        return detail(examId);
    }

    /**
     * 组装考试管理端响应列表
     * <p>批量查询试卷、班级、考生记录，统计各状态人数</p>
     */
    private List<ExamAdminRes> assemble(List<Exam> exams) {
        if (exams.isEmpty()) return List.of();
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, ExamPaper> papers = paperMapper.selectBatchIds(exams.stream().map(Exam::getPaperId).distinct().toList())
                .stream().collect(Collectors.toMap(ExamPaper::getId, Function.identity()));
        Map<Long, SysClass> classes = classMapper.selectBatchIds(exams.stream().map(Exam::getClassId).distinct().toList())
                .stream().collect(Collectors.toMap(SysClass::getId, Function.identity()));
        Map<Long, List<StudentExamRecord>> records = recordMapper.selectList(Wrappers.<StudentExamRecord>lambdaQuery()
                        .in(StudentExamRecord::getExamId, examIds)).stream()
                .collect(Collectors.groupingBy(StudentExamRecord::getExamId));
        LocalDateTime now = LocalDateTime.now();
        return exams.stream().map(exam -> {
            List<StudentExamRecord> list = records.getOrDefault(exam.getId(), List.of());
            ExamPaper paper = papers.get(exam.getPaperId()); SysClass clazz = classes.get(exam.getClassId());
            return new ExamAdminRes(exam.getId(), exam.getPaperId(), paper == null ? "未知试卷" : paper.getPaperName(),
                    exam.getClassId(), clazz == null ? "未知班级" : clazz.getClassPeriod(), exam.getStartTime(),
                    exam.getEntryDeadlineTime(), exam.getDurationMinutes(), exam.getCloseTime(),
                    exam.getInvigilatorUserId(), exam.getAnswerVisible(), ExamAdminRes.lifecycle(exam, now),
                    count(list, ExamRecordStatus.NOT_STARTED.name()), count(list, ExamRecordStatus.IN_PROGRESS.name()),
                    count(list, ExamRecordStatus.SUBMITTED.name()), count(list, ExamRecordStatus.TIMEOUT.name()),
                    count(list, ExamRecordStatus.ABSENT.name()), countGrading(list, GradingStatus.PENDING.name(), GradingStatus.GRADING.name()),
                    countGrading(list, GradingStatus.COMPLETED.name()));
        }).toList();
    }

    /** 统计指定考试状态的考生人数 */
    private long count(List<StudentExamRecord> records, String status) {
        return records.stream().filter(item -> status.equals(item.getStatus())).count();
    }
    /** 统计指定批改状态的考生人数（支持多状态） */
    private long countGrading(List<StudentExamRecord> records, String... statuses) {
        Set<String> values = Set.of(statuses);
        return records.stream().filter(item -> values.contains(item.getGradingStatus())).count();
    }
    /** 标准化筛选参数（去空格并转大写） */
    private String normalizeFilter(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }
    /** 根据ID获取考试，不存在则抛出业务异常 */
    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw BusinessException.DATA_ERROR.newInstance("考试不存在");
        return exam;
    }
    /** 根据ID获取试卷，不存在则抛出业务异常 */
    private ExamPaper requirePaper(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BusinessException.DATA_ERROR.newInstance("试卷不存在");
        return paper;
    }
    /** 根据ID获取考试记录，不存在则抛出业务异常 */
    private StudentExamRecord requireRecord(Long id) {
        StudentExamRecord record = recordMapper.selectById(id);
        if (record == null) throw BusinessException.DATA_ERROR.newInstance("考试记录不存在");
        return record;
    }
}
