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

/**
 * 试卷管理服务实现类
 * <p>提供试卷的分页查询、详情查看、创建和编辑功能，
 * 组卷时从题库快照题目和选项，保证试卷内容不受题库后续修改影响</p>
 */
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

    /**
     * 分页查询试卷列表
     *
     * @param req 分页查询参数（支持按关键词、课程、状态筛选）
     * @return 试卷分页结果
     */
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

    /**
     * 查看试卷详情（包含阶段、题目和选项信息）
     *
     * @param id 试卷ID
     * @return 试卷详情
     */
    @Override
    @Transactional(readOnly = true)
    public ExamPaperRes detail(Long id) {
        ExamPaper paper = requirePaper(id);
        return assemble(List.of(paper)).get(0);
    }

    /**
     * 创建试卷
     * <p>校验课程、阶段、题目合法性，创建试卷并快照题目和选项</p>
     *
     * @param req 试卷保存请求
     * @return 创建后的试卷详情
     */
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

    /**
     * 编辑试卷
     * <p>仅草稿状态可编辑，先删除旧的题目快照和阶段，再重新保存</p>
     *
     * @param id  试卷ID
     * @param req 试卷保存请求
     * @return 更新后的试卷详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperRes update(Long id, ExamPaperSaveReq req) {
        ExamPaper paper = requirePaper(id);
        // 只有草稿试卷可以修改
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
        // 删除旧的题目快照选项
        if (!paperQuestionIds.isEmpty()) {
            snapshotOptionMapper.delete(Wrappers.<ExamPaperQuestionOption>lambdaQuery()
                    .in(ExamPaperQuestionOption::getPaperQuestionId, paperQuestionIds));
        }
        // 删除旧的题目快照和阶段，重新保存
        paperQuestionMapper.delete(Wrappers.<ExamPaperQuestion>lambdaQuery().eq(ExamPaperQuestion::getPaperId, id));
        stageMapper.delete(Wrappers.<ExamPaperStage>lambdaQuery().eq(ExamPaperStage::getPaperId, id));
        saveComposition(id, req);
        return detail(id);
    }

    /**
     * 校验试卷保存请求的合法性
     * <p>校验课程存在、阶段不重复且属于课程、题目不重复、顺序不重复、题目已启用且属于对应课程阶段</p>
     */
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

    /**
     * 保存试卷组成（阶段 + 题目快照 + 选项快照）
     * <p>从题库复制题目和选项信息形成快照，保证试卷内容独立于题库</p>
     */
    private void saveComposition(Long paperId, ExamPaperSaveReq req) {
        // 保存试卷关联的课程阶段
        req.stageNames().stream().map(String::trim).distinct().forEach(name -> {
            ExamPaperStage stage = new ExamPaperStage(); stage.setPaperId(paperId); stage.setStageName(name); stageMapper.insert(stage);
        });
        // 批量查询题目和选项，准备快照
        Map<Long, ExamQuestion> questions = questionMapper.selectBatchIds(
                req.questions().stream().map(ExamPaperSaveReq.QuestionItem::questionId).toList()).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, item -> item));
        Map<Long, List<ExamQuestionOption>> options = questionOptionMapper.selectList(
                        Wrappers.<ExamQuestionOption>lambdaQuery()
                                .in(ExamQuestionOption::getQuestionId, questions.keySet())
                                .orderByAsc(ExamQuestionOption::getSortOrder))
                .stream().collect(Collectors.groupingBy(ExamQuestionOption::getQuestionId));
        // 按排序序号依次创建题目快照和选项快照
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

    /**
     * 组装试卷响应列表
     * <p>批量查询阶段、题目快照和选项快照，组装为完整响应</p>
     */
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

    /** 计算试卷总分（所有题目分值之和） */
    private BigDecimal total(ExamPaperSaveReq req) {
        return req.questions().stream().map(ExamPaperSaveReq.QuestionItem::questionScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 标准化筛选参数（去空格并转大写） */
    private String normalizeFilter(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /** 根据ID获取试卷，不存在则抛出业务异常 */
    private ExamPaper requirePaper(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BusinessException.DATA_ERROR.newInstance("试卷不存在");
        return paper;
    }
}
