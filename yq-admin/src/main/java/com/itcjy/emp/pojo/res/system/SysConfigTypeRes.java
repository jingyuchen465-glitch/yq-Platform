package com.itcjy.emp.pojo.res.system;

import com.itcjy.emp.pojo.entity.SysConfigType;

import java.time.LocalDateTime;

public record SysConfigTypeRes(Long id, String typeCode, String typeName, String description,
                               String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static SysConfigTypeRes from(SysConfigType type) {
        return new SysConfigTypeRes(type.getId(), type.getTypeCode(), type.getTypeName(),
                type.getDescription(), type.getStatus(), type.getCreatedAt(), type.getUpdatedAt());
    }
}
