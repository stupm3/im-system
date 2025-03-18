package com.stupm.common.enums;

import com.stupm.common.exception.ApplicationExceptionEnum;

public enum AddFriendReqErrorCode implements ApplicationExceptionEnum {
    AREADY_SEND(30009,"你已向对方发送过好友申请"),

    ;



    private int code;
    private String error;

    AddFriendReqErrorCode(int code, String error) {
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return code;
    }
    public String getError() {
        return error;
    }
}
