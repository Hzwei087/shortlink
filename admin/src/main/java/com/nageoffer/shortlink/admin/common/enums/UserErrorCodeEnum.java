package com.nageoffer.shortlink.admin.common.enums;

import com.nageoffer.shortlink.admin.common.convention.errorcode.IErrorCode;

public enum UserErrorCodeEnum implements IErrorCode {
//    USER_TOKEN_FAIL("A000200","用户TOKEN验证失败"),需要删除，网关里已定义

    USER_NULL("B000200","用户记录不存在"),
    USER_NAME_EXIST("B000201","用户名已存在"),
    USER_EXIST("B000202","用户记录已存在"),
    USER_SAVE_ERROR("B000203","用户保存失败");
//    USER_CODE_ERROR("B000204","用户密码错误"),
//    USER_ALREADY_LOGIN("B2000205","用户已登录"),
//    USER_UN_LOGIN("B2000205","用户未登录");需要删除，网关里已定义

    private final String code;

    private final String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
