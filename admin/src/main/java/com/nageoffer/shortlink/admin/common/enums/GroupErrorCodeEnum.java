package com.nageoffer.shortlink.admin.common.enums;

import com.nageoffer.shortlink.admin.common.convention.errorcode.IErrorCode;

public enum GroupErrorCodeEnum implements IErrorCode {
    GROUP_NAME_EXIST("B000300","组名已存在");


    private final String code;

    private final String message;

    GroupErrorCodeEnum(String code, String message) {
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
