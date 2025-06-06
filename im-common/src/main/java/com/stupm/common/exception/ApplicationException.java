package com.stupm.common.exception;


public class ApplicationException extends RuntimeException {

    private int code;

    private String error;


    public ApplicationException(int code, String message) {
        super(message);
        this.code = code;
        this.error = message;
    }

    public ApplicationException(ApplicationExceptionEnum exceptionEnum) {
        super(exceptionEnum.getError());
        this.code   = exceptionEnum.getCode();
        this.error = exceptionEnum.getError();
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }


    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
