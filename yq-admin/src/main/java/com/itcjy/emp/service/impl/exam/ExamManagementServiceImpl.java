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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ExamAdminRes> publish(ExamPublishReq req) {
        if (!req.startTime().isBefore(req.entryDeadlineTime())) {
            throw BusinessException.PARAMS_ERROR.newInstance("最晚入场时间必须晚于开始时间");
        }
        ExamPaper paper = requirePaper(req.paperId());
        if (PaperStatus.ARCHIVED.name().equals(paper.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("已归档试卷不能发布");
        }
        if (req.invigilatorUserId() != null && userMapper.selectById(req.invigilatorUserId()) == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("监考老师不存在");
        }
        List<Long> classIds = req.classIds().stream().distinct().toList();
        if (classIds.size() != req.classIds().size()) throw BusinessException.DATA_EXIST.newInstance("班级不能重复");
        List<SysClass> classes = classMapper.selectBatchIds(classIds);
        if (classes.size() != classIds.size()) throw BusinessException.CLAZZ_NOT_EXIST;
        classes.forEach(clazz -> {
            if (!paper.getCourseId().equals(clazz.getCourseId())) {
                throw BusinessException.DATA_ERROR.newInstance("班级课程与试卷课程不一致: " + clazz.getClassPeriod());
            }
        });
        LoginSession session = AuthThreadlocal.getLoginInfo();
        Long operatorId = session == null ? null : session.getPrincipalId();
        LocalDateTime closeTime = req.entryDeadlineTime().plusMinutes(req.durationMinutes());
        List<Long> examIds = new ArrayList<>();
        try {
            for (Long classId : classIds) {
                Exam exam = new Exam();
                exam.setPaperId(paper.getId()); exam.setClassId(classId); exam.setStartTime(req.startTime());
                exam.setEntryDeadlineTime(req.entryDeadlineTime()); exam.setDurationMinutes(req.durationMinutes());
                exam.setCloseTime(closeTime); exam.setInvigilatorUserId(req.invigilatorUserId());
                exam.setAnswerVisible(false); exam.setCreatedBy(operatorId); examMapper.insert(exam);
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
        if (PaperStatus.DRAFT.name().equals(paper.getStatus())) {
            paper.setStatus(PaperStatus.LOCKED.name()); paperMapper.updateById(paper);
        }
        return examIds.stream().map(this::detail).toList();
    }

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

    @Override
    @Transactional(readOnly = true)
    public ExamAdminRes detail(Long id) {
        Exam exam = requireExam(id);
        return assemble(List.of(exam)).get(0);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamGradingRes grade(Long recordId, ExamGradeReq req) {
        StudentExamRecord record = requireRecord(recordId);
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus()) || ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("考试尚未提交，不能批改");
        }
        Map<Long, StudentExamAnswer> answers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                        .eq(StudentExamAnswer::getRecordId, recordId)
                        .in(StudentExamAnswer::getQuestionType, QuestionType.FILL.name(), QuestionType.SHORT.name()))
                .stream().collect(Collectors.toMap(StudentExamAnswer::getId, Function.identity()));
        LoginSession session = AuthThreadlocal.getLoginInfo();
        Long graderId = session == null ? null : session.getPrincipalId();
        LocalDateTime now = LocalDateTime.now();
        for (ExamGradeReq.AnswerGrade grade : req.answers()) {
            StudentExamAnswer answer = answers.get(grade.answerId());
            if (answer == null) throw BusinessException.DATA_ERROR.newInstance("批改答案不属于该考试记录");
            if (grade.score().compareTo(answer.getQuestionScore()) > 0) {
                throw BusinessException.PARAMS_ERROR.newInstance("题目得分不能超过题目分值");
            }
            answer.setScore(grade.score()); answer.setGraderComment(StrUtil.trim(grade.comment()));
            answer.setGraderUserId(graderId); answer.setGradedAt(now); answerMapper.updateById(answer);
        }
        List<StudentExamAnswer> subjectiveAnswers = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId)
                .in(StudentExamAnswer::getQuestionType, QuestionType.FILL.name(), QuestionType.SHORT.name()));
        BigDecimal subjectiveScore = subjectiveAnswers.stream().map(StudentExamAnswer::getScore)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean completed = subjectiveAnswers.stream().allMatch(answer -> answer.getScore() != null);
        record.setSubjectiveScore(subjectiveScore);
        record.setScore(record.getObjectiveScore().add(subjectiveScore));
        record.setGradingStatus(completed ? GradingStatus.COMPLETED.name() : GradingStatus.GRADING.name());
        recordMapper.updateById(record);
        return gradingDetail(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamAdminRes updateVisibility(Long examId, ExamVisibilityReq req) {
        Exam exam = requireExam(examId);
        if (LocalDateTime.now().isBefore(exam.getCloseTime())) {
            throw BusinessException.DATA_ERROR.newInstance("整场考试关闭后才能公布答卷结果");
        }
        exam.setAnswerVisible(req.answerVisible());
        exam.setAnswerVisibleAt(Boolean.TRUE.equals(req.answerVisible()) ? LocalDateTime.now() : null);
        examMapper.updateById(exam);
        return detail(examId);
    }

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

    private long count(List<StudentExamRecord> records, String status) {
        return records.stream().filter(item -> status.equals(item.getStatus())).count();
    }
    private long countGrading(List<StudentExamRecord> records, String... statuses) {
        Set<String> values = Set.of(statuses);
        return records.stream().filter(item -> values.contains(item.getGradingStatus())).count();
    }
    private String normalizeFilter(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw BusinessException.DATA_ERROR.newInstance("考试不存在");
        return exam;
    }
    private ExamPaper requirePaper(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BusinessException.DATA_ERROR.newInstance("试卷不存在");
        return paper;
    }
    private StudentExamRecord requireRecord(Long id) {
        StudentExamRecord record = recordMapper.selectById(id);
        if (record == null) throw BusinessException.DATA_ERROR.newInstance("考试记录不存在");
        return record;
    }
}
