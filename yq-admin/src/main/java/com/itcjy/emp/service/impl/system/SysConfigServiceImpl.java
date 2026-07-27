package com.itcjy.emp.service.impl.system;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.myEnum.ConfigValueTypeEnum;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.system.SysConfigItemMapper;
import com.itcjy.emp.mapper.system.SysConfigTypeMapper;
import com.itcjy.emp.pojo.entity.SysConfigItem;
import com.itcjy.emp.pojo.entity.SysConfigType;
import com.itcjy.emp.pojo.req.system.ClassScheduleRuleReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemPageReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypePageReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypeReq;
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import com.itcjy.emp.pojo.res.system.SysConfigItemRes;
import com.itcjy.emp.pojo.res.system.SysConfigTypeRes;
import com.itcjy.emp.service.system.ISysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysConfigServiceImpl implements ISysConfigService {
    private static final String CLASS_SCHEDULE_RULE_CACHE_KEY = "sys_config:class_schedule_rule";
    private static final Duration CLASS_SCHEDULE_RULE_CACHE_TTL = Duration.ofHours(24);
    private static final Set<String> REQUIRED_SCHEDULE_KEYS = Set.of(
            ClassScheduleConstants.RuleKey.CLASS_DAYS,
            ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS,
            ClassScheduleConstants.RuleKey.REST_DAYS,
            ClassScheduleConstants.RuleKey.HOLIDAY_REST);

    private final SysConfigTypeMapper configTypeMapper;
    private final SysConfigItemMapper configItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addType(SysConfigTypeReq req) {
        String typeCode = normalizeCode(req.typeCode());
        ensureTypeCodeUnique(null, typeCode);
        SysConfigType type = new SysConfigType();
        applyType(type, req, typeCode);
        configTypeMapper.insert(type);
        invalidateScheduleRuleCacheAfterCommitIf(isScheduleType(type));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(Long id, SysConfigTypeReq req) {
        SysConfigType type = requireType(id);
        String typeCode = normalizeCode(req.typeCode());
        if (isScheduleType(type)
                && (!ClassScheduleConstants.CONFIG_TYPE_CODE.equals(typeCode)
                || !ActiveEnum.ACTIVE.name().equals(req.status()))) {
            throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则不能改编码或停用");
        }
        ensureTypeCodeUnique(id, typeCode);
        boolean affectsScheduleRule = isScheduleType(type)
                || ClassScheduleConstants.CONFIG_TYPE_CODE.equals(typeCode);
        applyType(type, req, typeCode);
        configTypeMapper.updateById(type);
        invalidateScheduleRuleCacheAfterCommitIf(affectsScheduleRule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        SysConfigType type = requireType(id);
        if (isScheduleType(type)) {
            throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则不能删除");
        }
        configItemMapper.delete(Wrappers.<SysConfigItem>lambdaQuery().eq(SysConfigItem::getTypeId, id));
        configTypeMapper.deleteById(id);
    }

    @Override
    public SysConfigTypeRes getType(Long id) {
        return SysConfigTypeRes.from(requireType(id));
    }

    @Override
    public PageResult<SysConfigTypeRes> pageTypes(SysConfigTypePageReq req) {
        IPage<SysConfigType> page = configTypeMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysConfigType>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getTypeCode()), SysConfigType::getTypeCode, req.getTypeCode())
                        .like(StrUtil.isNotBlank(req.getTypeName()), SysConfigType::getTypeName, req.getTypeName())
                        .eq(StrUtil.isNotBlank(req.getStatus()), SysConfigType::getStatus, req.getStatus())
                        .orderByAsc(SysConfigType::getId));
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(SysConfigTypeRes::from).toList());
    }

    @Override
    public List<SysConfigTypeRes> listTypes() {
        return configTypeMapper.selectList(Wrappers.<SysConfigType>lambdaQuery()
                        .orderByAsc(SysConfigType::getId)).stream()
                .map(SysConfigTypeRes::from).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(SysConfigItemReq req) {
        SysConfigType type = requireType(req.typeId());
        String itemKey = normalizeCode(req.itemKey());
        ensureItemKeyUnique(null, req.typeId(), itemKey);
        SysConfigItem item = new SysConfigItem();
        applyItem(item, req, itemKey);
        configItemMapper.insert(item);
        invalidateScheduleRuleCacheAfterCommitIf(isScheduleType(type));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, SysConfigItemReq req) {
        SysConfigItem item = requireItem(id);
        SysConfigType oldType = requireType(item.getTypeId());
        SysConfigType newType = requireType(req.typeId());
        String itemKey = normalizeCode(req.itemKey());
        if (isRequiredScheduleItem(oldType, item)) {
            boolean immutableChanged = !item.getTypeId().equals(req.typeId())
                    || !item.getItemKey().equals(itemKey)
                    || !item.getItemValue().equals(req.itemValue().trim())
                    || !item.getValueType().equals(req.valueType())
                    || !ActiveEnum.ACTIVE.name().equals(req.status());
            if (immutableChanged) {
                throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则值请使用排课规则整组更新接口修改");
            }
        }
        ensureItemKeyUnique(id, req.typeId(), itemKey);
        applyItem(item, req, itemKey);
        configItemMapper.updateById(item);
        invalidateScheduleRuleCacheAfterCommitIf(isScheduleType(oldType) || isScheduleType(newType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        SysConfigItem item = requireItem(id);
        SysConfigType type = requireType(item.getTypeId());
        if (isRequiredScheduleItem(type, item)) {
            throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则配置项不能删除");
        }
        configItemMapper.deleteById(id);
        invalidateScheduleRuleCacheAfterCommitIf(isScheduleType(type));
    }

    @Override
    public SysConfigItemRes getItem(Long id) {
        return SysConfigItemRes.from(requireItem(id));
    }

    @Override
    public PageResult<SysConfigItemRes> pageItems(SysConfigItemPageReq req) {
        IPage<SysConfigItem> page = configItemMapper.selectPage(new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysConfigItem>lambdaQuery()
                        .eq(req.getTypeId() != null, SysConfigItem::getTypeId, req.getTypeId())
                        .like(StrUtil.isNotBlank(req.getItemKey()), SysConfigItem::getItemKey, req.getItemKey())
                        .eq(StrUtil.isNotBlank(req.getStatus()), SysConfigItem::getStatus, req.getStatus())
                        .orderByAsc(SysConfigItem::getTypeId)
                        .orderByAsc(SysConfigItem::getSortOrder)
                        .orderByAsc(SysConfigItem::getId));
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(SysConfigItemRes::from).toList());
    }

    @Override
    public List<SysConfigItemRes> listItems(Long typeId) {
        requireType(typeId);
        return configItemMapper.selectList(Wrappers.<SysConfigItem>lambdaQuery()
                        .eq(SysConfigItem::getTypeId, typeId)
                        .orderByAsc(SysConfigItem::getSortOrder)
                        .orderByAsc(SysConfigItem::getId)).stream()
                .map(SysConfigItemRes::from).toList();
    }

    /**
     * 获取排课规则
     * @return
     */
    @Override
    public ClassScheduleRuleRes getClassScheduleRule() {
        ClassScheduleRuleRes cachedRule = getCachedClassScheduleRule();
        if (cachedRule != null) {
            return cachedRule;
        }

        ClassScheduleRuleRes databaseRule = loadClassScheduleRuleFromDatabase();
        cacheClassScheduleRule(databaseRule);
        return databaseRule;
    }

    private ClassScheduleRuleRes loadClassScheduleRuleFromDatabase() {
        SysConfigType type = configTypeMapper.selectOne(Wrappers.<SysConfigType>lambdaQuery()
                .eq(SysConfigType::getTypeCode, ClassScheduleConstants.CONFIG_TYPE_CODE)
                .eq(SysConfigType::getStatus, ActiveEnum.ACTIVE.name()));
        if (type == null) {
            throw BusinessException.CONFIG_ERROR.newInstance("排课规则配置类型不存在或未启用");
        }
        Map<String, SysConfigItem> items = loadScheduleItems(type.getId(), true);
        List<Integer> classDays = parseIntegerList(valueOf(items, ClassScheduleConstants.RuleKey.CLASS_DAYS));
        List<Integer> selfStudyDays = parseIntegerList(valueOf(items, ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS));
        List<Integer> restDays = parseIntegerList(valueOf(items, ClassScheduleConstants.RuleKey.REST_DAYS));
        boolean holidayRest = parseBoolean(valueOf(items, ClassScheduleConstants.RuleKey.HOLIDAY_REST));
        validateWeekRule(classDays, selfStudyDays, restDays);
        return new ClassScheduleRuleRes(classDays, selfStudyDays, restDays, holidayRest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClassScheduleRule(ClassScheduleRuleReq req) {
        validateWeekRule(req.classDays(), req.selfStudyDays(), req.restDays());
        SysConfigType type = configTypeMapper.selectOne(Wrappers.<SysConfigType>lambdaQuery()
                .eq(SysConfigType::getTypeCode, ClassScheduleConstants.CONFIG_TYPE_CODE));
        if (type == null || !ActiveEnum.ACTIVE.name().equals(type.getStatus())) {
            throw BusinessException.CONFIG_ERROR.newInstance("排课规则配置类型不存在或未启用");
        }
        Map<String, SysConfigItem> items = loadScheduleItems(type.getId(), false);
        updateItemValue(items, ClassScheduleConstants.RuleKey.CLASS_DAYS, joinDays(req.classDays()));
        updateItemValue(items, ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS, joinDays(req.selfStudyDays()));
        updateItemValue(items, ClassScheduleConstants.RuleKey.REST_DAYS, joinDays(req.restDays()));
        updateItemValue(items, ClassScheduleConstants.RuleKey.HOLIDAY_REST, req.holidayRest().toString());
        ClassScheduleRuleRes updatedRule = new ClassScheduleRuleRes(
                sortedDays(req.classDays()),
                sortedDays(req.selfStudyDays()),
                sortedDays(req.restDays()),
                req.holidayRest());
        runAfterCommit(() -> cacheClassScheduleRule(updatedRule));
    }

    private ClassScheduleRuleRes getCachedClassScheduleRule() {
        try {
            Map<Object, Object> cachedItems = redisTemplate.opsForHash()
                    .entries(CLASS_SCHEDULE_RULE_CACHE_KEY);
            if (cachedItems.isEmpty()) {
                return null;
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (String key : REQUIRED_SCHEDULE_KEYS) {
                Object value = cachedItems.get(key);
                if (value == null) {
                    log.warn("Incomplete class schedule rule cache, evicting key: {}",
                            CLASS_SCHEDULE_RULE_CACHE_KEY);
                    deleteScheduleRuleCache();
                    return null;
                }
                values.put(key, value.toString());
            }
            return toClassScheduleRule(values);
        } catch (BusinessException ex) {
            log.warn("Invalid class schedule rule cache, falling back to database", ex);
            deleteScheduleRuleCache();
            return null;
        } catch (DataAccessException ex) {
            log.warn("Failed to read class schedule rule cache, falling back to database", ex);
            return null;
        }
    }

    /**
     * 把排课日期规则缓存到Redis
     * @param rule
     */
    private void cacheClassScheduleRule(ClassScheduleRuleRes rule) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ClassScheduleConstants.RuleKey.CLASS_DAYS, joinDays(rule.classDays()));
        values.put(ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS, joinDays(rule.selfStudyDays()));
        values.put(ClassScheduleConstants.RuleKey.REST_DAYS, joinDays(rule.restDays()));
        values.put(ClassScheduleConstants.RuleKey.HOLIDAY_REST, Boolean.toString(rule.holidayRest()));
        try {
            redisTemplate.opsForHash().putAll(CLASS_SCHEDULE_RULE_CACHE_KEY, values);
            redisTemplate.expire(CLASS_SCHEDULE_RULE_CACHE_KEY, CLASS_SCHEDULE_RULE_CACHE_TTL);
        } catch (DataAccessException ex) {
            log.warn("Failed to cache class schedule rule", ex);
        }
    }

    private ClassScheduleRuleRes toClassScheduleRule(Map<String, String> values) {
        List<Integer> classDays = parseIntegerList(values.get(ClassScheduleConstants.RuleKey.CLASS_DAYS));
        List<Integer> selfStudyDays = parseIntegerList(values.get(ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS));
        List<Integer> restDays = parseIntegerList(values.get(ClassScheduleConstants.RuleKey.REST_DAYS));
        boolean holidayRest = parseBoolean(values.get(ClassScheduleConstants.RuleKey.HOLIDAY_REST));
        validateWeekRule(classDays, selfStudyDays, restDays);
        return new ClassScheduleRuleRes(classDays, selfStudyDays, restDays, holidayRest);
    }

    private void invalidateScheduleRuleCacheAfterCommitIf(boolean shouldInvalidate) {
        if (shouldInvalidate) {
            runAfterCommit(this::deleteScheduleRuleCache);
        }
    }

    private void deleteScheduleRuleCache() {
        try {
            redisTemplate.delete(CLASS_SCHEDULE_RULE_CACHE_KEY);
        } catch (DataAccessException ex) {
            log.warn("Failed to evict class schedule rule cache", ex);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void applyType(SysConfigType type, SysConfigTypeReq req, String typeCode) {
        type.setTypeCode(typeCode);
        type.setTypeName(req.typeName().trim());
        type.setDescription(StrUtil.trim(req.description()));
        type.setStatus(req.status());
    }

    private void applyItem(SysConfigItem item, SysConfigItemReq req, String itemKey) {
        item.setTypeId(req.typeId());
        item.setItemKey(itemKey);
        item.setValueType(req.valueType());
        item.setItemValue(normalizeValue(req.itemValue(), req.valueType()));
        item.setDescription(StrUtil.trim(req.description()));
        item.setStatus(req.status());
        item.setSortOrder(req.sortOrder());
    }

    private String normalizeValue(String value, String valueType) {
        String trimmed = value.trim();
        if (ConfigValueTypeEnum.BOOLEAN.name().equals(valueType)) {
            return Boolean.toString(parseBoolean(trimmed));
        }
        if (ConfigValueTypeEnum.INTEGER_LIST.name().equals(valueType)) {
            return joinDays(parseIntegerList(trimmed));
        }
        return trimmed;
    }

    private List<Integer> parseIntegerList(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        try {
            for (String part : value.split(",")) {
                result.add(Integer.valueOf(part.trim()));
            }
        } catch (NumberFormatException ex) {
            throw BusinessException.CONFIG_ERROR.newInstance("整数列表配置格式错误，应使用英文逗号分隔");
        }
        if (new HashSet<>(result).size() != result.size()) {
            throw BusinessException.CONFIG_ERROR.newInstance("整数列表配置不能包含重复值");
        }
        return List.copyOf(result);
    }

    private boolean parseBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw BusinessException.CONFIG_ERROR.newInstance("布尔配置值只能是 true 或 false");
        }
        return Boolean.parseBoolean(value);
    }

    private void validateWeekRule(List<Integer> classDays, List<Integer> selfStudyDays, List<Integer> restDays) {
        if (classDays == null || classDays.isEmpty() || selfStudyDays == null || restDays == null) {
            throw BusinessException.CONFIG_ERROR.newInstance("排课规则配置不完整");
        }
        List<Integer> allDays = new ArrayList<>();
        allDays.addAll(classDays);
        allDays.addAll(selfStudyDays);
        allDays.addAll(restDays);
        Set<Integer> uniqueDays = new HashSet<>(allDays);
        boolean validRange = allDays.stream().allMatch(day -> day != null && day >= 1 && day <= 7);
        if (!validRange || allDays.size() != uniqueDays.size() || !uniqueDays.equals(Set.of(1, 2, 3, 4, 5, 6, 7))) {
            throw BusinessException.CONFIG_ERROR.newInstance("上课日、自习日和休息日必须互斥并完整覆盖星期一到星期日");
        }
    }

    private Map<String, SysConfigItem> loadScheduleItems(Long typeId, boolean activeOnly) {
        List<SysConfigItem> list = configItemMapper.selectList(Wrappers.<SysConfigItem>lambdaQuery()
                .eq(SysConfigItem::getTypeId, typeId)
                .eq(activeOnly, SysConfigItem::getStatus, ActiveEnum.ACTIVE.name()));
        Map<String, SysConfigItem> result = new LinkedHashMap<>();
        for (SysConfigItem item : list) {
            result.put(item.getItemKey(), item);
        }
        if (!result.keySet().containsAll(REQUIRED_SCHEDULE_KEYS)) {
            throw BusinessException.CONFIG_ERROR.newInstance("排课规则配置项缺失或未启用");
        }
        return result;
    }

    private String valueOf(Map<String, SysConfigItem> items, String key) {
        return items.get(key).getItemValue();
    }

    private void updateItemValue(Map<String, SysConfigItem> items, String key, String value) {
        SysConfigItem item = items.get(key);
        item.setItemValue(value);
        configItemMapper.updateById(item);
    }

    private String joinDays(List<Integer> days) {
        return days.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> sortedDays(List<Integer> days) {
        return days.stream().sorted().toList();
    }

    private SysConfigType requireType(Long id) {
        SysConfigType type = configTypeMapper.selectById(id);
        if (type == null) {
            throw BusinessException.CONFIG_NOT_EXIST.newInstance("配置类型不存在");
        }
        return type;
    }

    private SysConfigItem requireItem(Long id) {
        SysConfigItem item = configItemMapper.selectById(id);
        if (item == null) {
            throw BusinessException.CONFIG_NOT_EXIST.newInstance("配置项不存在");
        }
        return item;
    }

    private void ensureTypeCodeUnique(Long id, String typeCode) {
        Long count = configTypeMapper.selectCount(Wrappers.<SysConfigType>lambdaQuery()
                .eq(SysConfigType::getTypeCode, typeCode)
                .ne(id != null, SysConfigType::getId, id));
        if (count > 0) {
            throw BusinessException.CONFIG_EXIST.newInstance("配置类型编码已存在");
        }
    }

    private void ensureItemKeyUnique(Long id, Long typeId, String itemKey) {
        Long count = configItemMapper.selectCount(Wrappers.<SysConfigItem>lambdaQuery()
                .eq(SysConfigItem::getTypeId, typeId)
                .eq(SysConfigItem::getItemKey, itemKey)
                .ne(id != null, SysConfigItem::getId, id));
        if (count > 0) {
            throw BusinessException.CONFIG_EXIST.newInstance("该配置类型下的配置项键已存在");
        }
    }

    private boolean isScheduleType(SysConfigType type) {
        return ClassScheduleConstants.CONFIG_TYPE_CODE.equals(type.getTypeCode());
    }

    private boolean isRequiredScheduleItem(SysConfigType type, SysConfigItem item) {
        return isScheduleType(type) && REQUIRED_SCHEDULE_KEYS.contains(item.getItemKey());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase();
    }
}
