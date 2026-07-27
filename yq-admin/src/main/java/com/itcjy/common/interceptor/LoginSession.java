package com.itcjy.common.interceptor;

import java.util.Collections;
import java.util.List;

/**
 * 员工端和学生端共享的最小登录会话契约。
 *
 * <p>跨端拦截器只依赖这些认证字段，不依赖任一业务端的详情对象。</p>
 */
public interface LoginSession {

    String getToken();

    String getSignSecret();

    Long getPrincipalId();

    String getPrincipalType();

    default List<String> getPermissions() {
        return Collections.emptyList();
    }
}
