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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamVO start(Long examId) {
        Long studentId = currentStudentId();
        Exam exam = requireExam(examId);
        StudentExamRecord record = recordMapper.selectOne(Wrappers.<StudentExamRecord>lambdaQuery()
                .eq(StudentExamRecord::getExamId, examId).eq(StudentExamRecord::getStudentId, studentId));
        if (record == null) throw BusinessException.DATA_ERROR.newInstance("你不在本场考试的考生名单中");
        LocalDateTime now = LocalDateTime.now();
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus())) {
            if (now.isBefore(exam.getStartTime())) throw BusinessException.DATA_ERROR.newInstance("考试尚未开始");
            if (now.isAfter(exam.getEntryDeadlineTime())) throw BusinessException.DATA_ERROR.newInstance("已超过最晚入场时间");
            LocalDateTime deadline = now.plusMinutes(exam.getDurationMinutes());
            int updated = recordMapper.update(null, Wrappers.<StudentExamRecord>lambdaUpdate()
                    .eq(StudentExamRecord::getId, record.getId())
                    .eq(StudentExamRecord::getStatus, ExamRecordStatus.NOT_STARTED.name())
                    .set(StudentExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                    .set(StudentExamRecord::getStartTime, now)
                    .set(StudentExamRecord::getDeadlineTime, deadline));
            record = recordMapper.selectById(record.getId());
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

    @Override
    @Transactional(readOnly = true)
    public StudentExamVO detail(Long recordId) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        if (ExamRecordStatus.NOT_STARTED.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("请先进入考试");
        }
        return buildExam(requireExam(record.getExamId()), record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long recordId, Long paperQuestionId, StudentExamAnswerDTO dto) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        if (!ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("当前考试不能继续作答");
        }
        if (record.getDeadlineTime() == null || LocalDateTime.now().isAfter(record.getDeadlineTime())) {
            throw BusinessException.DATA_ERROR.newInstance("答题时间已结束，系统正在自动交卷");
        }
        StudentExamAnswer answer = answerMapper.selectOne(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId)
                .eq(StudentExamAnswer::getPaperQuestionId, paperQuestionId));
        if (answer == null) throw BusinessException.DATA_ERROR.newInstance("题目不属于当前试卷");
        ExamPaperQuestion question = questionMapper.selectById(paperQuestionId);
        if (question == null || !record.getExamId().equals(answer.getExamId())
                || !question.getPaperId().equals(answer.getPaperId())) {
            throw BusinessException.DATA_ERROR.newInstance("题目不属于当前试卷");
        }
        answer.setAnswerContent(normalizeAnswer(question, dto.answerContent()));
        answerMapper.updateById(answer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentExamVO submit(Long recordId) {
        StudentExamRecord owned = requireOwnedRecord(recordId);
        if (ExamRecordStatus.IN_PROGRESS.name().equals(owned.getStatus())) {
            submissionService.submit(recordId, ExamSubmitReason.MANUAL);
        }
        StudentExamRecord updated = requireOwnedRecord(recordId);
        return buildExam(requireExam(updated.getExamId()), updated);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentExamResultVO result(Long recordId) {
        StudentExamRecord record = requireOwnedRecord(recordId);
        Exam exam = requireExam(record.getExamId());
        if (LocalDateTime.now().isBefore(exam.getCloseTime()) || !Boolean.TRUE.equals(exam.getAnswerVisible())) {
            throw BusinessException.DATA_ERROR.newInstance("考试结果尚未公布");
        }
        if (!GradingStatus.COMPLETED.name().equals(record.getGradingStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("答卷仍在批改中");
        }
        ExamPaper paper = requirePaper(exam.getPaperId());
        List<ExamPaperQuestion> questions = listQuestions(paper.getId());
        Map<Long, StudentExamAnswer> answers = listAnswers(recordId).stream()
                .collect(Collectors.toMap(StudentExamAnswer::getPaperQuestionId, Function.identity()));
        return StudentExamResultVO.from(exam, paper, record, questions, answers);
    }

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

    private List<ExamPaperQuestion> listQuestions(Long paperId) {
        return questionMapper.selectList(Wrappers.<ExamPaperQuestion>lambdaQuery()
                .eq(ExamPaperQuestion::getPaperId, paperId).orderByAsc(ExamPaperQuestion::getSortOrder));
    }
    private List<StudentExamAnswer> listAnswers(Long recordId) {
        return answerMapper.selectList(Wrappers.<StudentExamAnswer>lambdaQuery()
                .eq(StudentExamAnswer::getRecordId, recordId).orderByAsc(StudentExamAnswer::getPaperQuestionId));
    }
    private Long currentStudentId() { return loginService.getCurrentStudent().getId(); }
    private StudentExamRecord requireOwnedRecord(Long recordId) {
        StudentExamRecord record = recordMapper.selectById(recordId);
        if (record == null || !currentStudentId().equals(record.getStudentId())) {
            throw BusinessException.DATA_ERROR.newInstance("考试记录不存在");
        }
        return record;
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
}
