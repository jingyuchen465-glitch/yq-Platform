package com.itcjy.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public static final BusinessException ACCOUNT_NOT_ACTIVE = new BusinessException(10000, "Account not active");
    public static final BusinessException USER_EXIST = new BusinessException(10001, "User already exists");
    public static final BusinessException USER_NOT_EXIST = new BusinessException(10002, "User not found");
    public static final BusinessException PERMISSION_EXIST = new BusinessException(11001, "Permission already exists");
    public static final BusinessException PERMISSION_NOT_EXIST = new BusinessException(11002, "Permission not found");
    public static final BusinessException PARAMS_ERROR = new BusinessException(11005, "Invalid parameters");
    public static final BusinessException PASSWORD_ERROR = new BusinessException(11003, "Password incorrect");
    public static final BusinessException DATA_EXIST = new BusinessException(11006, "Data already exists");
    public static final BusinessException DATA_ERROR = new BusinessException(11004, "Data error");
    public static final BusinessException ROLE_EXIST = new BusinessException(12001, "Role already exists");
    public static final BusinessException ROLE_NOT_EXIST = new BusinessException(12002, "Role not found");
    public static final BusinessException CONFIG_EXIST = new BusinessException(13001, "Config already exists");
    public static final BusinessException CONFIG_NOT_EXIST = new BusinessException(13002, "Config not found");
    public static final BusinessException CONFIG_ERROR = new BusinessException(13003, "Invalid config");
    public static final BusinessException CAMPUS_NOT_EXIST = new BusinessException(14001, "Campus not found");
    public static final BusinessException COURSE_NOT_EXIST = new BusinessException(15001, "Course not found");
    public static final BusinessException COURSE_DETAIL_NOT_EXIST = new BusinessException(15002, "Course detail not found");
    public static final BusinessException CLAZZ_NOT_EXIST = new BusinessException(16001, "Class not found");
    public static final BusinessException CLASS_SCHEDULE_ERROR = new BusinessException(16002, "Class schedule error");
    public static final BusinessException TEACHER_SCHEDULE_CONFLICT = new BusinessException(16003, "Teacher schedule conflict");
    public static final BusinessException TEMPORARY_COURSE_ERROR = new BusinessException(16004, "Temporary course error");
    public static final BusinessException CLASS_SCHEDULE_NOT_EXIST = new BusinessException(16005, "Class schedule not found");
    public static final BusinessException DUTY_NOT_EXIST = new BusinessException(16006, "Duty not found");
    public static final BusinessException DUTY_CONFLICT = new BusinessException(16007, "Duty conflict");
    public static final BusinessException PRODUCT_NOT_EXIST = new BusinessException(17001, "Product not found");
    public static final BusinessException PREPAY_ORDER_NOT_EXIST = new BusinessException(17002, "Prepay order not found");
    public static final BusinessException REMOTE_ERROR = new BusinessException(17003, "Remote call failed");
    public static final BusinessException JWT_ERROR = new BusinessException(17004, "Token校验失败");
    public static final BusinessException JWT_EXPIRE = new BusinessException(17005, "Token已过期");
    public static final BusinessException USER_NO_TOKEN = new BusinessException(17006, "用户未登录或登录已失效");

    // 签名相关
    public static final BusinessException REQUEST_HEADER_ERROR = new BusinessException(18001, "请求头签名参数缺失");
    public static final BusinessException SIGN_NONCE_REPEAT = new BusinessException(18002, "请求不可重复提交");
    public static final BusinessException SIGN_SECRET_NOT_FOUND = new BusinessException(18003, "签名密钥不存在");
    public static final BusinessException SIGN_ERROR = new BusinessException(18004, "签名验证失败");
    public static final BusinessException SIGN_EXPIRE = new BusinessException(18005, "签名已过期");
    public static final BusinessException PARSE_TIMESTAMP_ERROR = new BusinessException(18006, "时间戳格式错误");


    public static final BusinessException HOMEWORK_EXIST = new BusinessException(19001, "Homework already exists");
    public static final BusinessException HOMEWORK_SCHEDULE_NOT_EXIST = new BusinessException(19002, "Homework schedule not found");
    public static final BusinessException HOMEWORK_TIME_ERROR = new BusinessException(19003, "Invalid homework time range");
    public static final BusinessException HOMEWORK_NOT_EXIST = new BusinessException(19004, "Homework not found");
    public static final BusinessException HOMEWORK_ANSWER_NOT_EXIST = new BusinessException(19005, "Homework answer not found");
    public static final BusinessException HOMEWORK_SUBMISSION_NOT_EXIST = new BusinessException(19006, "Homework submission not found");
    public static final BusinessException STUDENT_EXIST = new BusinessException(20001, "Student already exists");
    public static final BusinessException STUDENT_NOT_EXIST = new BusinessException(20002, "Student not found");

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException newInstance(String message) {
        return new BusinessException(this.getCode(), message);
    }
}
