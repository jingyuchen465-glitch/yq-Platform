package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.service.exam.*;
import com.itcjy.stu.pojo.DTO.StudentExamAnswerDTO;
import com.itcjy.stu.pojo.VO.*;
import com.itcjy.stu.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生端考试服务实现类
 * <p>提供学生查看考试列表、进入考试、保存答案、手动交卷和查看成绩等功能</p>
 */
@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {
    private final LoginService loginService;
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper questionMapper;
    private final ExamPaperQuestionOptionMapper optionMapper;
    private final StudentExamRecordMapper recordMapper;
    private final StudentExamAnswerMapper answerMapper;
    private final IExamTimeoutService timeoutService;
    private final IExamSubmissionService submissionService;

    /**
     * 查询当前学生的所有考试列表
     *
     * @return 考试列表（包含考试状态、成绩可见性等信息）
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentExamListVO> listCurrentStudentExams() {
        Long studentId = currentStudentId();
        List<StudentExamRecord> records = recordMapper.selectList(Wrappers.<StudentExamRecord>lambdaQuery()
                .eq(StudentExamRecord::getStudentId, studentId).orderByDesc(StudentExamRecord::getId));
        if (records.isEmpty()) return List.of();
        Map<Long, Exam> exams = examMapper.selectBatchIds(records.stream().map(StudentExamRecord::getExamId).toList())
                .stream().collect(Collectors.toMap(Exam::getId, Function.identity()));
        Map<Long, ExamPaper> papers = paperMapper.selectBatchIds(exams.values().stream().map(Exam::getPaperId).distinct().toList())
                .stream().collect(Collectors.toMap(ExamPaper::getId, Function.identity()));
        return records.stream().map(record -> {
            Exam exam = exams.get(record.getExamId());
            return StudentExamListVO.from(exam, papers.get(exam.getPaperId()), record);
        }).toList();
    }

    /**
     * 开始考试
     * <p>校验考试时间和入场截止，更新记录状态为进行中，
     * 创建空白答题记录并调度超时交卷任务</p>
     *
     * @param examId 考试ID
     * @return 考试详情（包含题目和已保存的答案）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamVO start(Long examId) {
        Long studentId = currentStudentId();
        Exam exam = requireExam(examId);
        StudentExamRecord record = recordMapper.selectOne(Wrappers.<StudentExamRecord>lambdaQuery()
                .eq(StudentExamRecord::getExamId, examId).eq(StudentExamRecord::getStudentId, studentId));
        // 校验学生是否在考生名单中
        if (record == null) throw BusinessException.DATA_ERROR.newInstance("你不在本场考试的考生名单中");
        LocalDateTime now = LocalDateTime.now();
        // 首次进入考试：校验时间并更新状态
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus())) {
            // 校验考试是否已开始
            if (now.isBefore(exam.getStartTime())) throw BusinessException.DATA_ERROR.newInstance("考试尚未开始");
            // 校验是否超过最晚入场时间
            if (now.isAfter(exam.getEntryDeadlineTime())) throw BusinessException.DATA_ERROR.newInstance("已超过最晚入场时间");
            // 计算个人答题截止时间
            LocalDateTime deadline = now.plusMinutes(exam.getDurationMinutes());
            // 乐观更新：仅当状态仍为 NOT_STARTED 时才更新
            int updated = recordMapper.update(null, Wrappers.<StudentExamRecord>lambdaUpdate()
                    .eq(StudentExamRecord::getId, record.getId())
                    .eq(StudentExamRecord::getStatus, ExamRecordStatus.NOT_STARTED.name())
                    .set(StudentExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                    .set(StudentExamRecord::getStartTime, now)
                    .set(StudentExamRecord::getDeadlineTime, deadline));
            record = recordMapper.selectById(record.getId());
            // 更新成功后创建空白答题记录并调度超时任务
            if (updated == 1) {
                createBlankAnswers(record, exam);
                timeoutService.schedule(record.getId(), deadline);
            }
        }
        if (ExamRecordStatus.ABSENT.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("该考试已记为缺考");
        }
        return buildExam(exam, record);
    }

    /**
     * 查看考试详情（答题页面数据）
     *
     * @param recordId 考试记录ID
     * @return 考试详情（包含题目、选项和已保存的答案）
     */
    @Override
    @Transactional(readOnly = true)
    public StudentExamVO detail(Long recordId) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        // 未开始的考试不能查看详情
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("请先进入考试");
        }
        return buildExam(requireExam(record.getExamId()), record);
    }

    /**
     * 保存单题答案
     * <p>校验考试状态、答题时间、题目归属，标准化答案后保存</p>
     *
     * @param recordId        考试记录ID
     * @param paperQuestionId 试卷题目ID
     * @param dto             答案内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long recordId, Long paperQuestionId, StudentExamAnswerDTO dto) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        // 校验考试是否在进行中
        if (!ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("当前考试不能继续作答");
        }
        // 校验答题时间是否已截止
        if (record.getDeadlineTime() == null || LocalDateTime.now().isAfter(record.getDeadlineTime())) {
            throw BusinessException.DATA_ERROR.newInstance("答题时间已结束，系统正在自动交卷");
        }
        // 校验题目属于当前试卷
        StudentExamAnswer answer = answerMapper.selectOne(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId)
                .eq(StudentExamAnswer::getPaperQuestionId, paperQuestionId));
        if (answer == null) throw BusinessException.DATA_ERROR.newInstance("题目不属于当前试卷");
        ExamPaperQuestion question = questionMapper.selectById(paperQuestionId);
        if (question == null || !record.getExamId().equals(answer.getExamId())
                || !question.getPaperId().equals(answer.getPaperId())) {
            throw BusinessException.DATA_ERROR.newInstance("题目不属于当前试卷");
        }
        // 标准化答案并保存
        answer.setAnswerContent(normalizeAnswer(question, dto.answerContent()));
        answerMapper.updateById(answer);
    }

    /**
     * 手动交卷
     *
     * @param recordId 考试记录ID
     * @return 交卷后的考试详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamVO submit(Long recordId) {
        StudentExamRecord owned = requireOwnedRecord(recordId);
        // 仅进行中的考试可以交卷
        if (ExamRecordStatus.IN_PROGRESS.name().equals(owned.getStatus())) {
            submissionService.submit(recordId, ExamSubmitReason.MANUAL);
        }
        StudentExamRecord updated = requireOwnedRecord(recordId);
        return buildExam(requireExam(updated.getExamId()), updated);
    }

    /**
     * 查看考试成绩
     * <p>仅在考试关闭、结果已公布且批改完成后可查看</p>
     *
     * @param recordId 考试记录ID
     * @return 成绩详情（包含每题的得分和解析）
     */
    @Override
    @Transactional(readOnly = true)
    public StudentExamResultVO result(Long recordId) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        Exam exam = requireExam(record.getExamId());
        // 校验考试已关闭且结果已公布
        if (LocalDateTime.now().isBefore(exam.getCloseTime()) || !Boolean.TRUE.equals(exam.getAnswerVisible())) {
            throw BusinessException.DATA_ERROR.newInstance("考试结果尚未公布");
        }
        // 校验批改已完成
        if (!GradingStatus.COMPLETED.name().equals(record.getGradingStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("答卷仍在批改中");
        }
        ExamPaper paper = requirePaper(exam.getPaperId());
        List<ExamPaperQuestion> questions = listQuestions(paper.getId());
        Map<Long, StudentExamAnswer> answers = listAnswers(recordId).stream()
                .collect(Collectors.toMap(StudentExamAnswer::getPaperQuestionId, Function.identity()));
        return StudentExamResultVO.from(exam, paper, record, questions, answers);
    }

    /**
     * 创建空白答题记录
     * <p>为试卷中的每道题创建一条空白答题记录，已存在的跳过（幂等）</p>
     */
    private void createBlankAnswers(StudentExamRecord record, Exam exam) {
        List<ExamPaperQuestion> questions = listQuestions(exam.getPaperId());
        Set<Long> existingQuestionIds = answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                        .select(StudentExamAnswer::getPaperQuestionId)
                        .eq(StudentExamAnswer::getRecordId, record.getId()))
                .stream().map(StudentExamAnswer::getPaperQuestionId).collect(Collectors.toSet());
        for (ExamPaperQuestion question : questions) {
            if (existingQuestionIds.contains(question.getId())) {
                continue;
            }
            StudentExamAnswer answer = new StudentExamAnswer();
            answer.setRecordId(record.getId()); answer.setExamId(exam.getId()); answer.setPaperId(exam.getPaperId());
            answer.setPaperQuestionId(question.getId()); answer.setQuestionId(question.getQuestionId());
            answer.setQuestionType(question.getQuestionType()); answer.setQuestionScore(question.getQuestionScore());
            answerMapper.insert(answer);
        }
    }

    /**
     * 构建考试详情 VO
     * <p>组装试卷题目、选项和学生已保存的答案</p>
     */
    private StudentExamVO buildExam(Exam exam, StudentExamRecord record) {
        ExamPaper paper = requirePaper(exam.getPaperId());
        List<ExamPaperQuestion> questions = listQuestions(paper.getId());
        List<Long> questionIds = questions.stream().map(ExamPaperQuestion::getId).toList();
        Map<Long, List<ExamPaperQuestionOption>> options = questionIds.isEmpty() ? Map.of() :
                optionMapper.selectList(Wrappers.<ExamPaperQuestionOption>lambdaQuery()
                                .in(ExamPaperQuestionOption::getPaperQuestionId, questionIds)
                                .orderByAsc(ExamPaperQuestionOption::getSortOrder))
                        .stream().collect(Collectors.groupingBy(ExamPaperQuestionOption::getPaperQuestionId));
        Map<Long, StudentExamAnswer> answers = listAnswers(record.getId()).stream()
                .collect(Collectors.toMap(StudentExamAnswer::getPaperQuestionId, Function.identity()));
        return StudentExamVO.from(exam, paper, record, questions, options, answers);
    }

    /**
     * 标准化学生答案
     * <p>多选题去重排序，单选/判断转大写，并校验选项存在性</p>
     */
    private String normalizeAnswer(ExamPaperQuestion question, String raw) {
        String value = raw == null ? "" : raw.trim();
        QuestionType type = QuestionType.valueOf(question.getQuestionType());
        if (type == QuestionType.MULTIPLE) {
            value = Arrays.stream(value.toUpperCase().split(",")).map(String::trim)
                    .filter(item -> !item.isBlank()).distinct().sorted().collect(Collectors.joining(","));
        } else if (type == QuestionType.SINGLE || type == QuestionType.JUDGE) value = value.toUpperCase();
        if (type == QuestionType.SINGLE && value.contains(",")) {
            throw BusinessException.PARAMS_ERROR.newInstance("单选题只能选择一个答案");
        }
        if (type == QuestionType.JUDGE && !value.isEmpty() && !Set.of("TRUE", "FALSE").contains(value)) {
            throw BusinessException.PARAMS_ERROR.newInstance("判断题答案不合法");
        }
        if (type.choice() && !value.isEmpty()) {
            Set<String> optionKeys = optionMapper.selectList(Wrappers.<ExamPaperQuestionOption>lambdaQuery()
                            .eq(ExamPaperQuestionOption::getPaperQuestionId, question.getId()))
                    .stream().map(ExamPaperQuestionOption::getOptionKey).collect(Collectors.toSet());
            if (!optionKeys.containsAll(Arrays.asList(value.split(",")))) {
                throw BusinessException.PARAMS_ERROR.newInstance("选择的选项不存在");
            }
        }
        return value;
    }

    /** 查询试卷的所有题目（按排序序号升序） */
    private List<ExamPaperQuestion> listQuestions(Long paperId) {
        return questionMapper.selectList(Wrappers.<ExamPaperQuestion>lambdaQuery()
                .eq(ExamPaperQuestion::getPaperId, paperId).orderByAsc(ExamPaperQuestion::getSortOrder));
    }
    /** 查询考试记录的所有答题记录 */
    private List<StudentExamAnswer> listAnswers(Long recordId) {
        return answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId).orderByAsc(StudentExamAnswer::getPaperQuestionId));
    }
    /** 获取当前登录学生ID */
    private Long currentStudentId() { return loginService.getCurrentStudent().getId(); }
    /** 获取当前学生拥有的考试记录，不存在或非本人则抛出异常 */
    private StudentExamRecord requireOwnedRecord(Long recordId) {
        StudentExamRecord record = recordMapper.selectById(recordId);
        if (record == null || !currentStudentId().equals(record.getStudentId())) {
            throw BusinessException.DATA_ERROR.newInstance("考试记录不存在");
        }
        return record;
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
}
