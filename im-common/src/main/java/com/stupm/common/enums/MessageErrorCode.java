package com.stupm.common.enums;


import com.stupm.common.exception.ApplicationExceptionEnum;

public enum MessageErrorCode implements ApplicationExceptionEnum {


    FROMER_IS_FORBIDDEN(50003, "发送方被禁用"),

    FROMER_IS_MUTE(50002, "发送方被禁言"),


    MESSAGEBODY_IS_NOT_EXIST(50003, "消息体不存在"),

    MESSAGE_IS_RECALLED(50005, "消息已被撤回"),

    MESSAGE_RECALL_TIME_OUT(50004, "消息已超过可撤回时间"),

    ;

    private int code;
    private String error;

    MessageErrorCode(int code, String error) {
        this.code = code;
        this.error = error;
    }

    public int getCode() {
        return this.code;
    }

    public String getError() {
        return this.error;
    }

}
