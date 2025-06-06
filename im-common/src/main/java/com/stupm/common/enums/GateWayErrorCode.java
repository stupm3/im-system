package com.stupm.common.enums;


import com.stupm.common.exception.ApplicationExceptionEnum;

public enum GateWayErrorCode implements ApplicationExceptionEnum {

    USERSIGN_NOT_EXIST(60000,"用户签名不存在"),

    APPID_NOT_EXIST(60001,"appId不存在"),

    OPERATER_NOT_EXIST(60002,"操作人不存在"),

    USERSIGN_IS_ERROR(60003,"用户签名不正确"),

    USERSIGN_OPERATE_NOT_MATE(60005,"用户签名与操作人不匹配"),

    USERSIGN_IS_EXPIRED(60004,"用户签名已过期"),

    ;

    private int code;
    private String error;

    GateWayErrorCode(int code, String error){
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
