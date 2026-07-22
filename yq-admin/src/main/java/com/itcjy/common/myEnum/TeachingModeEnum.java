package com.itcjy.common.myEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TeachingModeEnum {

    ONLINE("线上"),

    OFFLINE("线下");

    @Getter
    private final String desc;
}
