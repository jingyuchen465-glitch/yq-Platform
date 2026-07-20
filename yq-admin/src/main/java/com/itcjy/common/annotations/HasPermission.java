package com.itcjy.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HasPermission {

    // 权限码
    String code();

    // 权限名称
    String name();

    // 权限描述
    String description() default "";
}