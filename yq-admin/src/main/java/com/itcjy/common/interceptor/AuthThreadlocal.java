package com.itcjy.common.interceptor;

/**
 * 登录信息 ThreadLocal，用于在请求链路中传递当前用户信息。
 */
public final class AuthThreadlocal {

    private static final ThreadLocal<LoginSession> HOLDER = new ThreadLocal<>();

    private AuthThreadlocal() {
    }

    public static void setLoginInfo(LoginSession loginInfo) {
        HOLDER.set(loginInfo);
    }

    public static LoginSession getLoginInfo() {
        return HOLDER.get();
    }

    public static void remove() {
        HOLDER.remove();
    }
}
