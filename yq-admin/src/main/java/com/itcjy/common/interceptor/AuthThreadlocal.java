package com.itcjy.common.interceptor;

import com.itcjy.emp.pojo.res.LoginInfo;

/**
 * 登录信息 ThreadLocal，用于在请求链路中传递当前用户信息。
 */
public class AuthThreadlocal {

    private static final ThreadLocal<LoginInfo> HOLDER = new ThreadLocal<>();

    public static void setLoginInfo(LoginInfo loginInfo) {
        HOLDER.set(loginInfo);
    }

    public static LoginInfo getLoginInfo() {
        return HOLDER.get();
    }

    public static void remove() {
        HOLDER.remove();
    }
}
