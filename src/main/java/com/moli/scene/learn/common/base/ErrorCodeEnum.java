package com.moli.scene.learn.common.base;

public enum ErrorCodeEnum {
    SUCCESS, FAIL, FAIL_DATA;

    private Integer errorCode;
    private String errorDesc;

    public static void ifExistsCode(Integer code) {

    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }
}
