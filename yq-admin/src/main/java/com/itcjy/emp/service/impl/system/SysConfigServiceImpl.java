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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements ISysConfigService {

    private static final Set<String> REQUIRED_SCHEDULE_KEYS = Set.of(
            ClassScheduleConstants.RuleKey.CLASS_DAYS,
            ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS,
            ClassScheduleConstants.RuleKey.REST_DAYS,
            ClassScheduleConstants.RuleKey.HOLIDAY_REST);

    private final SysConfigTypeMapper configTypeMapper;
    private final SysConfigItemMapper configItemMapper;

    @Override
    public void addType(SysConfigTypeReq req) {
        String typeCode = normalizeCode(req.typeCode());
        ensureTypeCodeUnique(null, typeCode);
        SysConfigType type = new SysConfigType();
        applyType(type, req, typeCode);
        configTypeMapper.insert(type);
    }

    @Override
    public void updateType(Long id, SysConfigTypeReq req) {
        SysConfigType type = requireType(id);
        String typeCode = normalizeCode(req.typeCode());
        if (isScheduleType(type)
                && (!ClassScheduleConstants.CONFIG_TYPE_CODE.equals(typeCode)
                || !ActiveEnum.ACTIVE.name().equals(req.status()))) {
            throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则不能改编码或停用");
        }
        ensureTypeCodeUnique(id, typeCode);
        applyType(type, req, typeCode);
        configTypeMapper.updateById(type);
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
    public void addItem(SysConfigItemReq req) {
        requireType(req.typeId());
        String itemKey = normalizeCode(req.itemKey());
        ensureItemKeyUnique(null, req.typeId(), itemKey);
        SysConfigItem item = new SysConfigItem();
        applyItem(item, req, itemKey);
        configItemMapper.insert(item);
    }

    @Override
    public void updateItem(Long id, SysConfigItemReq req) {
        SysConfigItem item = requireItem(id);
        SysConfigType oldType = requireType(item.getTypeId());
        requireType(req.typeId());
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
    }

    @Override
    public void deleteItem(Long id) {
        SysConfigItem item = requireItem(id);
        if (isRequiredScheduleItem(requireType(item.getTypeId()), item)) {
            throw BusinessException.CONFIG_ERROR.newInstance("内置排课规则配置项不能删除");
        }
        configItemMapper.deleteById(id);
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

    @Override
    public ClassScheduleRuleRes getClassScheduleRule() {
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
