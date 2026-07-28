package com.itcjy.emp.service.impl.exam;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.*;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.*;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamPaperRes;
import com.itcjy.emp.service.exam.IExamPaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements IExamPaperService {
    private final ExamPaperMapper paperMapper;
    private final ExamPaperStageMapper stageMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamPaperQuestionOptionMapper snapshotOptionMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamQuestionOptionMapper questionOptionMapper;
    private final ExamQuestionCourseMapper questionCourseMapper;
    private final SysCourseMapper courseMapper;
    private final SysCourseDetailMapper courseDetailMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamPaperRes> page(ExamPaperPageReq req) {
        String status = normalizeFilter(req.getStatus());
        IPage<ExamPaper> page = paperMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<ExamPaper>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getKeyword()), ExamPaper::getPaperName, StrUtil.trim(req.getKeyword()))
                        .eq(req.getCourseId() != null, ExamPaper::getCourseId, req.getCourseId())
                        .eq(status != null, ExamPaper::getStatus, status)
                        .orderByDesc(ExamPaper::getUpdatedAt).orderByDesc(ExamPaper::getId));
        return new PageResult<>(page.getTotal(), assemble(page.getRecords()));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPaperRes detail(Long id) {
        ExamPaper paper = requirePaper(id);
        return assemble(List.of(paper)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperRes create(ExamPaperSaveReq req) {
        validate(req);
        ExamPaper paper = new ExamPaper();
        paper.setPaperName(req.paperName().trim());
        paper.setCourseId(req.courseId());
        paper.setTotalScore(total(req));
        paper.setStatus(PaperStatus.DRAFT.name());
        LoginSession session = AuthThreadlocal.getLoginInfo();
        paper.setCreatedBy(session == null ? null : session.getPrincipalId());
        paperMapper.insert(paper);
        saveComposition(paper.getId(), req);
        return detail(paper.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperRes update(Long id, ExamPaperSaveReq req) {
        ExamPaper paper = requirePaper(id);
        if (!PaperStatus.DRAFT.name().equals(paper.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("只有草稿试卷可以修改");
        }
        validate(req);
        paper.setPaperName(req.paperName().trim());
        paper.setCourseId(req.courseId());
        paper.setTotalScore(total(req));
        paperMapper.updateById(paper);
        List<Long> paperQuestionIds = paperQuestionMapper.selectList(Wrappers.<ExamPaperQuestion>lambdaQuery()
                        .select(ExamPaperQuestion::getId).eq(ExamPaperQuestion::getPaperId, id))
                .stream().map(ExamPaperQuestion::getId).toList();
        if (!paperQuestionIds.isEmpty()) {
            snapshotOptionMapper.delete(Wrappers.<ExamPaperQuestionOption>lambdaQuery()
                    .in(ExamPaperQuestionOption::getPaperQuestionId, paperQuestionIds));
        }
        paperQuestionMapper.delete(Wrappers.<ExamPaperQuestion>lambdaQuery().eq(ExamPaperQuestion::getPaperId, id));
        stageMapper.delete(Wrappers.<ExamPaperStage>lambdaQuery().eq(ExamPaperStage::getPaperId, id));
        saveComposition(id, req);
        return detail(id);
    }

    private void validate(ExamPaperSaveReq req) {
        if (courseMapper.selectById(req.courseId()) == null) throw BusinessException.COURSE_NOT_EXIST;
        Set<String> stages = req.stageNames().stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        if (stages.size() != req.stageNames().size()) throw BusinessException.DATA_EXIST.newInstance("试卷阶段不能重复");
        for (String stage : stages) {
            if (!courseDetailMapper.exists(Wrappers.<SysCourseDetail>lambdaQuery()
                    .eq(SysCourseDetail::getCourseId, req.courseId()).eq(SysCourseDetail::getStageName, stage))) {
                throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("课程阶段不存在: " + stage);
            }
        }
        Set<Long> questionIds = req.questions().stream().map(ExamPaperSaveReq.QuestionItem::questionId)
                .collect(Collectors.toSet());
        if (questionIds.size() != req.questions().size()) throw BusinessException.DATA_EXIST.newInstance("试卷题目不能重复");
        Set<Integer> orders = req.questions().stream().map(ExamPaperSaveReq.QuestionItem::sortOrder).collect(Collectors.toSet());
        if (orders.size() != req.questions().size()) throw BusinessException.DATA_EXIST.newInstance("题目顺序不能重复");
        Map<Long, ExamQuestion> questions = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, item -> item));
        if (questions.size() != questionIds.size()) throw BusinessException.DATA_ERROR.newInstance("部分题目不存在");
        Set<Long> related = questionCourseMapper.selectList(Wrappers.<ExamQuestionCourse>lambdaQuery()
                        .in(ExamQuestionCourse::getQuestionId, questionIds)
                        .eq(ExamQuestionCourse::getCourseId, req.courseId())
                        .in(ExamQuestionCourse::getStageName, stages))
                .stream().map(ExamQuestionCourse::getQuestionId).collect(Collectors.toSet());
        for (Long questionId : questionIds) {
            ExamQuestion question = questions.get(questionId);
            if (!QuestionStatus.ENABLED.name().equals(question.getStatus())) {
                throw BusinessException.DATA_ERROR.newInstance("停用题目不能加入试卷: " + questionId);
            }
            if (!related.contains(questionId)) {
                throw BusinessException.DATA_ERROR.newInstance("题目不属于试卷所选课程阶段: " + questionId);
            }
        }
    }

    private void saveComposition(Long paperId, ExamPaperSaveReq req) {
        req.stageNames().stream().map(String::trim).distinct().forEach(name -> {
            ExamPaperStage stage = new ExamPaperStage(); stage.setPaperId(paperId); stage.setStageName(name); stageMapper.insert(stage);
        });
        Map<Long, ExamQuestion> questions = questionMapper.selectBatchIds(
                req.questions().stream().map(ExamPaperSaveReq.QuestionItem::questionId).toList()).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, item -> item));
        Map<Long, List<ExamQuestionOption>> options = questionOptionMapper.selectList(
                        Wrappers.<ExamQuestionOption>lambdaQuery()
                                .in(ExamQuestionOption::getQuestionId, questions.keySet())
                                .orderByAsc(ExamQuestionOption::getSortOrder))
                .stream().collect(Collectors.groupingBy(ExamQuestionOption::getQuestionId));
        req.questions().stream().sorted(Comparator.comparing(ExamPaperSaveReq.QuestionItem::sortOrder)).forEach(item -> {
            ExamQuestion source = questions.get(item.questionId());
            ExamPaperQuestion snapshot = new ExamPaperQuestion();
            snapshot.setPaperId(paperId); snapshot.setQuestionId(source.getId());
            snapshot.setQuestionType(source.getQuestionType()); snapshot.setQuestionContent(source.getQuestionContent());
            snapshot.setAnswerContent(source.getAnswerContent()); snapshot.setAnalysisContent(source.getAnalysisContent());
            snapshot.setDifficulty(source.getDifficulty()); snapshot.setQuestionScore(item.questionScore());
            snapshot.setSortOrder(item.sortOrder()); paperQuestionMapper.insert(snapshot);
            options.getOrDefault(source.getId(), List.of()).forEach(option -> {
                ExamPaperQuestionOption copy = new ExamPaperQuestionOption();
                copy.setPaperQuestionId(snapshot.getId()); copy.setOptionKey(option.getOptionKey());
                copy.setOptionContent(option.getOptionContent()); copy.setSortOrder(option.getSortOrder());
                snapshotOptionMapper.insert(copy);
            });
        });
    }

    private List<ExamPaperRes> assemble(List<ExamPaper> papers) {
        if (papers.isEmpty()) return List.of();
        List<Long> paperIds = papers.stream().map(ExamPaper::getId).toList();
        Map<Long, List<ExamPaperStage>> stages = stageMapper.selectList(Wrappers.<ExamPaperStage>lambdaQuery()
                        .in(ExamPaperStage::getPaperId, paperIds).orderByAsc(ExamPaperStage::getId))
                .stream().collect(Collectors.groupingBy(ExamPaperStage::getPaperId));
        List<ExamPaperQuestion> questionList = paperQuestionMapper.selectList(Wrappers.<ExamPaperQuestion>lambdaQuery()
                .in(ExamPaperQuestion::getPaperId, paperIds).orderByAsc(ExamPaperQuestion::getSortOrder));
        Map<Long, List<ExamPaperQuestion>> questions = questionList.stream().collect(Collectors.groupingBy(ExamPaperQuestion::getPaperId));
        List<Long> questionIds = questionList.stream().map(ExamPaperQuestion::getId).toList();
        Map<Long, List<ExamPaperQuestionOption>> options = questionIds.isEmpty() ? Map.of() :
                snapshotOptionMapper.selectList(Wrappers.<ExamPaperQuestionOption>lambdaQuery()
                                .in(ExamPaperQuestionOption::getPaperQuestionId, questionIds)
                                .orderByAsc(ExamPaperQuestionOption::getSortOrder))
                        .stream().collect(Collectors.groupingBy(ExamPaperQuestionOption::getPaperQuestionId));
        return papers.stream().map(paper -> ExamPaperRes.from(paper,
                stages.getOrDefault(paper.getId(), List.of()), questions.getOrDefault(paper.getId(), List.of()), options)).toList();
    }

    private BigDecimal total(ExamPaperSaveReq req) {
        return req.questions().stream().map(ExamPaperSaveReq.QuestionItem::questionScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeFilter(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private ExamPaper requirePaper(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BusinessException.DATA_ERROR.newInstance("试卷不存在");
        return paper;
    }
}
