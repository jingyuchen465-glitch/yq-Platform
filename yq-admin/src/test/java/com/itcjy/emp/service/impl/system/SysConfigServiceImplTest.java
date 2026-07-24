package com.itcjy.emp.service.impl.system;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.mapper.system.SysConfigItemMapper;
import com.itcjy.emp.mapper.system.SysConfigTypeMapper;
import com.itcjy.emp.pojo.entity.SysConfigItem;
import com.itcjy.emp.pojo.entity.SysConfigType;
import com.itcjy.emp.pojo.req.system.ClassScheduleRuleReq;
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigTypeMapper configTypeMapper;
    @Mock
    private SysConfigItemMapper configItemMapper;

    private SysConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(SysConfigType.class);
        initTableInfo(SysConfigItem.class);
        service = new SysConfigServiceImpl(configTypeMapper, configItemMapper);
    }

    @Test
    void shouldReadClassScheduleRuleFromDatabaseItems() {
        SysConfigType type = scheduleType();
        when(configTypeMapper.selectOne(any())).thenReturn(type);
        when(configItemMapper.selectList(any())).thenReturn(List.of(
                item(1L, type.getId(), ClassScheduleConstants.RuleKey.CLASS_DAYS, "1,2,3,5,6"),
                item(2L, type.getId(), ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS, "4"),
                item(3L, type.getId(), ClassScheduleConstants.RuleKey.REST_DAYS, "7"),
                item(4L, type.getId(), ClassScheduleConstants.RuleKey.HOLIDAY_REST, "true")));

        ClassScheduleRuleRes result = service.getClassScheduleRule();

        assertThat(result.classDays()).containsExactly(1, 2, 3, 5, 6);
        assertThat(result.selfStudyDays()).containsExactly(4);
        assertThat(result.restDays()).containsExactly(7);
        assertThat(result.holidayRest()).isTrue();
    }

    @Test
    void shouldRejectIncompleteClassScheduleRule() {
        SysConfigType type = scheduleType();
        when(configTypeMapper.selectOne(any())).thenReturn(type);
        when(configItemMapper.selectList(any())).thenReturn(List.of(
                item(1L, type.getId(), ClassScheduleConstants.RuleKey.CLASS_DAYS, "1,2,3,5,6")));

        assertThatThrownBy(service::getClassScheduleRule)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺失");
    }

    @Test
    void shouldRejectOverlappingWeekDaysBeforeUpdatingDatabase() {
        ClassScheduleRuleReq req = new ClassScheduleRuleReq(
                List.of(1, 2, 3, 4, 5), List.of(4, 6), List.of(7), true);

        assertThatThrownBy(() -> service.updateClassScheduleRule(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("互斥");
        verify(configTypeMapper, never()).selectOne(any());
    }

    @Test
    void shouldUpdateFourScheduleItemsInOneOperation() {
        SysConfigType type = scheduleType();
        when(configTypeMapper.selectOne(any())).thenReturn(type);
        when(configItemMapper.selectList(any())).thenReturn(List.of(
                item(1L, type.getId(), ClassScheduleConstants.RuleKey.CLASS_DAYS, "1,2,3,5,6"),
                item(2L, type.getId(), ClassScheduleConstants.RuleKey.SELF_STUDY_DAYS, "4"),
                item(3L, type.getId(), ClassScheduleConstants.RuleKey.REST_DAYS, "7"),
                item(4L, type.getId(), ClassScheduleConstants.RuleKey.HOLIDAY_REST, "true")));
        ClassScheduleRuleReq req = new ClassScheduleRuleReq(
                List.of(1, 2, 3, 4, 5), List.of(6), List.of(7), false);

        service.updateClassScheduleRule(req);

        ArgumentCaptor<SysConfigItem> captor = ArgumentCaptor.forClass(SysConfigItem.class);
        verify(configItemMapper, org.mockito.Mockito.times(4)).updateById(captor.capture());
        assertThat(captor.getAllValues()).extracting(SysConfigItem::getItemValue)
                .containsExactly("1,2,3,4,5", "6", "7", "false");
    }

    @Test
    void shouldProtectBuiltInScheduleTypeFromDeletion() {
        when(configTypeMapper.selectById(1L)).thenReturn(scheduleType());

        assertThatThrownBy(() -> service.deleteType(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
        verify(configTypeMapper, never()).deleteById(1L);
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityType);
        }
    }

    private SysConfigType scheduleType() {
        SysConfigType type = new SysConfigType();
        type.setId(1L);
        type.setTypeCode(ClassScheduleConstants.CONFIG_TYPE_CODE);
        type.setTypeName("排课规则");
        type.setStatus(ActiveEnum.ACTIVE.name());
        return type;
    }

    private SysConfigItem item(Long id, Long typeId, String key, String value) {
        SysConfigItem item = new SysConfigItem();
        item.setId(id);
        item.setTypeId(typeId);
        item.setItemKey(key);
        item.setItemValue(value);
        item.setStatus(ActiveEnum.ACTIVE.name());
        return item;
    }
}
