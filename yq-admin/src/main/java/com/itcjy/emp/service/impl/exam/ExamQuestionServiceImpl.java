package com.itcjy.emp.service.impl.exam;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamQuestionRes;
import com.itcjy.emp.service.exam.IExamQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题库管理服务实现类
 * <p>提供题目的分页查询、详情查看、创建、编辑和启停用功能，
 * 支持按课程阶段、题型、难度、状态等多维度筛选</p>
 */
@Service
@RequiredArgsConstructor
public class ExamQuestionServiceImpl implements IExamQuestionService {
    private final ExamQuestionMapper questionMapper;
    private final ExamQuestionOptionMapper optionMapper;
    private final ExamQuestionCourseMapper courseRelationMapper;
    private final SysCourseMapper courseMapper;
    private final SysCourseDetailMapper courseDetailMapper;

    /**
     * 分页查询题库题目列表
     * <p>支持按关键词、课程、阶段、题型、难度、状态筛选</p>
     *
     * @param req 分页查询参数
     * @return 题目分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamQuestionRes> page(ExamQuestionPageReq req) {
        // 先通过课程阶段关联关系筛选出符合条件的题目ID集合
        List<Long> relationQuestionIds = findRelationQuestionIds(req);
        if (relationQuestionIds != null && relationQuestionIds.isEmpty()) {
            return new PageResult<>(0L, List.of());
        }
        String questionType = normalizeFilter(req.getQuestionType());
        String difficulty = normalizeFilter(req.getDifficulty());
        String status = normalizeFilter(req.getStatus());
        LambdaQueryWrapper<ExamQuestion> query = Wrappers.<ExamQuestion>lambdaQuery()
                .like(StrUtil.isNotBlank(req.getKeyword()), ExamQuestion::getQuestionContent, StrUtil.trim(req.getKeyword()))
                .eq(questionType != null, ExamQuestion::getQuestionType, questionType)
                .eq(difficulty != null, ExamQuestion::getDifficulty, difficulty)
                .eq(status != null, ExamQuestion::getStatus, status)
                .in(relationQuestionIds != null, ExamQuestion::getId, relationQuestionIds)
                .orderByDesc(ExamQuestion::getUpdatedAt)
                .orderByDesc(ExamQuestion::getId);
        IPage<ExamQuestion> page = questionMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()), query);
        return new PageResult<>(page.getTotal(), assemble(page.getRecords()));
    }

    /**
     * 查看题目详情（包含选项和课程阶段关联）
     *
     * @param id 题目ID
     * @return 题目详情
     */
    @Override
    @Transactional(readOnly = true)
    public ExamQuestionRes detail(Long id) {
        ExamQuestion question = requireQuestion(id);
        return ExamQuestionRes.from(question, listOptions(id), listCourseStages(id));
    }

    /**
     * 创建题目
     * <p>校验题型、难度、课程阶段、选项合法性，创建题目并保存关联关系</p>
     *
     * @param req 题目保存请求
     * @return 创建后的题目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionRes create(ExamQuestionSaveReq req) {
        NormalizedQuestion normalized = validate(req);
        ExamQuestion question = new ExamQuestion();
        fillQuestion(question, req, normalized);
        question.setStatus(QuestionStatus.ENABLED.name());
        questionMapper.insert(question);
        replaceRelations(question.getId(), req);
        return detail(question.getId());
    }

    /**
     * 编辑题目
     * <p>更新题目基本信息，删除旧的选项和课程关联后重新保存</p>
     *
     * @param id  题目ID
     * @param req 题目保存请求
     * @return 更新后的题目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionRes update(Long id, ExamQuestionSaveReq req) {
        ExamQuestion question = requireQuestion(id);
        NormalizedQuestion normalized = validate(req);
        fillQuestion(question, req, normalized);
        questionMapper.updateById(question);
        optionMapper.delete(Wrappers.<ExamQuestionOption>lambdaQuery().eq(ExamQuestionOption::getQuestionId, id));
        courseRelationMapper.delete(Wrappers.<ExamQuestionCourse>lambdaQuery().eq(ExamQuestionCourse::getQuestionId, id));
        replaceRelations(id, req);
        return detail(id);
    }

    /**
     * 更新题目状态（启用/停用）
     *
     * @param id  题目ID
     * @param req 状态请求
     * @return 更新后的题目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionRes updateStatus(Long id, ExamQuestionStatusReq req) {
        ExamQuestion question = requireQuestion(id);
        QuestionStatus status = enumValue(QuestionStatus.class, req.status(), "题目状态不合法");
        question.setStatus(status.name());
        questionMapper.updateById(question);
        return detail(id);
    }

    /**
     * 根据课程和阶段条件查找关联的题目ID集合
     *
     * @return 题目ID列表，无条件时返回 null 表示不限制
     */
    private List<Long> findRelationQuestionIds(ExamQuestionPageReq req) {
        if (req.getCourseId() == null && StrUtil.isBlank(req.getStageName())) return null;
        return courseRelationMapper.selectList(Wrappers.<ExamQuestionCourse>lambdaQuery()
                        .eq(req.getCourseId() != null, ExamQuestionCourse::getCourseId, req.getCourseId())
                        .eq(StrUtil.isNotBlank(req.getStageName()), ExamQuestionCourse::getStageName, StrUtil.trim(req.getStageName())))
                .stream().map(ExamQuestionCourse::getQuestionId).distinct().toList();
    }

    /**
     * 组装题目响应列表
     * <p>批量查询选项和课程关联，组装为完整响应</p>
     */
    private List<ExamQuestionRes> assemble(List<ExamQuestion> questions) {
        if (questions.isEmpty()) return List.of();
        List<Long> ids = questions.stream().map(ExamQuestion::getId).toList();
        Map<Long, List<ExamQuestionOption>> options = optionMapper.selectList(
                        Wrappers.<ExamQuestionOption>lambdaQuery().in(ExamQuestionOption::getQuestionId, ids)
                                .orderByAsc(ExamQuestionOption::getSortOrder).orderByAsc(ExamQuestionOption::getId))
                .stream().collect(Collectors.groupingBy(ExamQuestionOption::getQuestionId));
        Map<Long, List<ExamQuestionCourse>> courses = courseRelationMapper.selectList(
                        Wrappers.<ExamQuestionCourse>lambdaQuery().in(ExamQuestionCourse::getQuestionId, ids)
                                .orderByAsc(ExamQuestionCourse::getCourseId).orderByAsc(ExamQuestionCourse::getStageName))
                .stream().collect(Collectors.groupingBy(ExamQuestionCourse::getQuestionId));
        return questions.stream().map(q -> ExamQuestionRes.from(q,
                options.getOrDefault(q.getId(), List.of()), courses.getOrDefault(q.getId(), List.of()))).toList();
    }

    /**
     * 保存题目的选项和课程阶段关联关系
     */
    private void replaceRelations(Long questionId, ExamQuestionSaveReq req) {
        if (req.options() != null) {
            req.options().forEach(item -> {
                ExamQuestionOption option = new ExamQuestionOption();
                option.setQuestionId(questionId);
                option.setOptionKey(item.optionKey().trim().toUpperCase());
                option.setOptionContent(item.optionContent().trim());
                option.setSortOrder(item.sortOrder());
                optionMapper.insert(option);
            });
        }
        req.courseStages().forEach(item -> {
            ExamQuestionCourse relation = new ExamQuestionCourse();
            relation.setQuestionId(questionId);
            relation.setCourseId(item.courseId());
            relation.setStageName(item.stageName().trim());
            courseRelationMapper.insert(relation);
        });
    }

    /**
     * 校验题目保存请求的合法性
     * <p>校验题型、难度、课程阶段存在且不重复、选择题选项和答案合法性</p>
     *
     * @return 标准化后的题目信息（题型、难度、答案）
     */
    private NormalizedQuestion validate(ExamQuestionSaveReq req) {
        QuestionType type = enumValue(QuestionType.class, req.questionType(), "题型不合法");
        QuestionDifficulty difficulty = enumValue(QuestionDifficulty.class, req.difficulty(), "难度不合法");
        Set<String> stageKeys = new HashSet<>();
        req.courseStages().forEach(item -> {
            if (courseMapper.selectById(item.courseId()) == null) throw BusinessException.COURSE_NOT_EXIST;
            boolean stageExists = courseDetailMapper.exists(Wrappers.lambdaQuery(com.itcjy.emp.pojo.entity.SysCourseDetail.class)
                    .eq(com.itcjy.emp.pojo.entity.SysCourseDetail::getCourseId, item.courseId())
                    .eq(com.itcjy.emp.pojo.entity.SysCourseDetail::getStageName, item.stageName().trim()));
            if (!stageExists) throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("课程阶段不存在: " + item.stageName());
            if (!stageKeys.add(item.courseId() + "#" + item.stageName().trim())) {
                throw BusinessException.DATA_EXIST.newInstance("课程阶段不能重复");
            }
        });
        List<ExamQuestionSaveReq.OptionItem> options = req.options() == null ? List.of() : req.options();
        if (type.choice()) {
            if (options.size() < 2) throw BusinessException.PARAMS_ERROR.newInstance("选择题至少需要两个选项");
            Set<String> keys = options.stream().map(item -> item.optionKey().trim().toUpperCase()).collect(Collectors.toSet());
            if (keys.size() != options.size()) throw BusinessException.DATA_EXIST.newInstance("选项标识不能重复");
            String answer = normalizeAnswer(type, req.answerContent());
            if (!keys.containsAll(Arrays.asList(answer.split(",")))) {
                throw BusinessException.PARAMS_ERROR.newInstance("标准答案必须来自现有选项");
            }
            if (type == QuestionType.SINGLE && answer.contains(",")) {
                throw BusinessException.PARAMS_ERROR.newInstance("单选题只能有一个答案");
            }
            return new NormalizedQuestion(type, difficulty, answer);
        }
        if (!options.isEmpty()) throw BusinessException.PARAMS_ERROR.newInstance("非选择题不能配置选项");
        String answer = type == QuestionType.JUDGE ? normalizeAnswer(type, req.answerContent()) : req.answerContent().trim();
        return new NormalizedQuestion(type, difficulty, answer);
    }

    /**
     * 标准化答案格式
     * <p>判断题转为大写 TRUE/FALSE，选择题去重排序后以逗号拼接</p>
     */
    private String normalizeAnswer(QuestionType type, String raw) {
        if (type == QuestionType.JUDGE) {
            String value = raw.trim().toUpperCase();
            if (!Set.of("TRUE", "FALSE").contains(value)) {
                throw BusinessException.PARAMS_ERROR.newInstance("判断题答案只能是 TRUE 或 FALSE");
            }
            return value;
        }
        return Arrays.stream(raw.toUpperCase().split(","))
                .map(String::trim).filter(StrUtil::isNotBlank).distinct().sorted().collect(Collectors.joining(","));
    }

    /** 填充题目实体的基本字段 */
    private void fillQuestion(ExamQuestion question, ExamQuestionSaveReq req, NormalizedQuestion normalized) {
        question.setQuestionType(normalized.type().name());
        question.setQuestionContent(req.questionContent().trim());
        question.setAnswerContent(normalized.answer());
        question.setAnalysisContent(StrUtil.trim(req.analysisContent()));
        question.setDifficulty(normalized.difficulty().name());
    }

    /** 根据ID获取题目，不存在则抛出业务异常 */
    private ExamQuestion requireQuestion(Long id) {
        ExamQuestion question = questionMapper.selectById(id);
        if (question == null) throw BusinessException.DATA_ERROR.newInstance("题目不存在");
        return question;
    }

    /** 查询题目的选项列表（按排序序号升序） */
    private List<ExamQuestionOption> listOptions(Long id) {
        return optionMapper.selectList(Wrappers.<ExamQuestionOption>lambdaQuery()
                .eq(ExamQuestionOption::getQuestionId, id).orderByAsc(ExamQuestionOption::getSortOrder));
    }

    /** 查询题目的课程阶段关联列表 */
    private List<ExamQuestionCourse> listCourseStages(Long id) {
        return courseRelationMapper.selectList(Wrappers.<ExamQuestionCourse>lambdaQuery()
                .eq(ExamQuestionCourse::getQuestionId, id).orderByAsc(ExamQuestionCourse::getCourseId)
                .orderByAsc(ExamQuestionCourse::getStageName));
    }

    /** 安全解析枚举值，解析失败抛出业务异常 */
    private <T extends Enum<T>> T enumValue(Class<T> type, String value, String message) {
        try { return Enum.valueOf(type, value.trim().toUpperCase()); }
        catch (Exception ex) { throw BusinessException.PARAMS_ERROR.newInstance(message); }
    }

    /** 标准化筛选参数（去空格并转大写） */
    private String normalizeFilter(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /** 标准化后的题目信息（用于校验通过后填充实体） */
    private record NormalizedQuestion(QuestionType type, QuestionDifficulty difficulty, String answer) {}
}
