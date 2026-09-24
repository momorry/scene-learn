package com.moli.scene.learn.common.base;

public enum ErrorCodeEnum {
    SUCCESS(200, "ok"), FAIL(500, "fail"), FAIL_DATA(501, "failData");

    private Integer errorCode;
    private String errorDesc;

    ErrorCodeEnum(Integer errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

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
