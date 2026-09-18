package com.moli.scene.learn.common.base;

import lombok.Data;

import java.io.Serializable;

/**
 */
@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = -449118551698164456L;

    /**
     * 200 成功 非200错误
     */
    private Integer code;

    private String msg;

    private T data;

    /**
     * 当失败时code=501，如果需要返回某些数据
     */
    private Object data4Fail;

    /***
     * 构建ApiResult 对象 - （有返回内容）
     *
     * @param errorCodeEnum 状态码
     * @param data 返回内容
     * @return
     */
    private static <T> ApiResult<T> response(T data, ErrorCodeEnum errorCodeEnum) {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(errorCodeEnum.getErrorCode());
        apiResult.setData(data);
        apiResult.setMsg(errorCodeEnum.getErrorDesc());
        return apiResult;
    }

    /***
     * 构建ApiResult 对象 - （有返回内容）
     *
     * @param code 状态码
     * @param msg 返回信息
     * @param data 返回内容
     * @return
     */
    private static <T> ApiResult<T> response(T data, Integer code, String msg) {
        ErrorCodeEnum.ifExistsCode(code);
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    /***
     * 构建ApiResult 对象 - （返回内容）
     *
     * @param code 状态码
     * @param msg 返回信息
     * @return
     */
    private static <T> ApiResult<T> response(Integer code, String msg) {
        ErrorCodeEnum.ifExistsCode(code);
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(code);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public static <T> ApiResult<T> ok() {
        return response(null, ErrorCodeEnum.SUCCESS);
    }

    public static <T> ApiResult<T> ok(T data) {
        return response(data, ErrorCodeEnum.SUCCESS);
    }

    public static <T> ApiResult<T> ok(T data, String msg) {
        return response(data, ErrorCodeEnum.SUCCESS.getErrorCode(), msg);
    }

    public static <T> ApiResult<T> fail() {
        return response(null, ErrorCodeEnum.FAIL);
    }

    public static <T> ApiResult<T> fail(String msg) {
        return response(null, ErrorCodeEnum.FAIL.getErrorCode(), msg);
    }

    public static <T> ApiResult<T> failData(Object data4Fail) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ErrorCodeEnum.FAIL_DATA.getErrorCode());
        result.setMsg(ErrorCodeEnum.FAIL_DATA.getErrorDesc());
        result.data4Fail = data4Fail;
        return result;
    }

    public static <T> ApiResult<T> failData(Object data4Fail, String msg) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ErrorCodeEnum.FAIL_DATA.getErrorCode());
        result.setMsg(msg);
        result.data4Fail = data4Fail;
        return result;
    }

    public static <T> ApiResult<T> fail(Integer code, String msg) {
        return response(null, code, msg);
    }

    public static <T> ApiResult<T> securityFail(Integer code, String msg) {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(code);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public static ApiResult<String> unAuth() {
        ApiResult<String> apiResult = new ApiResult<>();
        apiResult.setCode(401);
        apiResult.setMsg("用户未认证");
        return apiResult;
    }

    public static ApiResult<String> unPermission() {
        ApiResult<String> apiResult = new ApiResult<>();
        apiResult.setCode(403);
        apiResult.setMsg("用户无权限");
        return apiResult;
    }

    /**
     * 无权限时提示连续管理员
     * @return
     */
    public static ApiResult<String> unPermissionWithPerson() {
        ApiResult<String> apiResult = new ApiResult<>();
        apiResult.setCode(403);
        apiResult.setMsg("您暂无访问权限，请联系平台管理员");
        return apiResult;
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
