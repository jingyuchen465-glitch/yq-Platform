package com.itcjy.emp.pojo.res.system;

import com.itcjy.emp.pojo.entity.SysConfigItem;

import java.time.LocalDateTime;

public record SysConfigItemRes(Long id, Long typeId, String itemKey, String itemValue,
                               String valueType, String description, String status, Integer sortOrder,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static SysConfigItemRes from(SysConfigItem item) {
        return new SysConfigItemRes(item.getId(), item.getTypeId(), item.getItemKey(), item.getItemValue(),
                item.getValueType(), item.getDescription(), item.getStatus(), item.getSortOrder(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
}
