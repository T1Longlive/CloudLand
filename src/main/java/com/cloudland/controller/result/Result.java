package com.cloudland.controller.result;

/**
 * 统一响应结果封装类
 */
public class Result {
    /**
     * 响应数据
     */
    private Object data;

    /**
     * 业务状态码（使用Code类中的常量）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private Object msg;

    // ==================== 构造方法 ====================

    public Result(Integer code, Object data) {
        this.data = data;
        this.code = code;
    }

    public Result(Integer code, Object data, String msg) {
        this.data = data;
        this.code = code;
        this.msg = msg;
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 成功响应（无数据）
     * @return Result对象
     */
    public static Result success() {
        return new Result(Code.SUCCESS, null, "操作成功");
    }

    /**
     * 成功响应（带数据）
     * @param data 响应数据
     * @return Result对象
     */
    public static Result success(Object data) {
        return new Result(Code.SUCCESS, data, "操作成功");
    }

    /**
     * 成功响应（带数据和消息）
     * @param data 响应数据
     * @param msg 响应消息
     * @return Result对象
     */
    public static Result success(Object data, String msg) {
        return new Result(Code.SUCCESS, data, msg);
    }

    /**
     * 成功响应（带状态码、数据和消息）
     * @param code 业务状态码
     * @param data 响应数据
     * @param msg 响应消息
     * @return Result对象
     */
    public static Result success(Integer code, Object data, String msg) {
        return new Result(code, data, msg);
    }

    /**
     * 失败响应（仅消息）
     * @param msg 错误消息
     * @return Result对象
     */
    public static Result failure(String msg) {
        return new Result(Code.FAILURE, null, msg);
    }

    /**
     * 失败响应（带状态码和消息）
     * @param code 业务状态码
     * @param msg 错误消息
     * @return Result对象
     */
    public static Result failure(Integer code, String msg) {
        return new Result(code, null, msg);
    }

    /**
     * 失败响应（带状态码、数据和消息）
     * @param code 业务状态码
     * @param data 响应数据
     * @param msg 错误消息
     * @return Result对象
     */
    public static Result failure(Integer code, Object data, String msg) {
        return new Result(code, data, msg);
    }

    /**
     * 错误响应（兼容旧版本）
     * @param msg 错误消息
     * @return Result对象
     * @deprecated 使用 failure(String msg) 替代
     */
    @Deprecated
    public static Result error(String msg) {
        return failure(msg);
    }

    // ==================== Getter/Setter ====================

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    // ==================== 工具方法 ====================

    /**
     * 判断当前结果是否成功
     * @return true-成功, false-失败
     */
    public boolean isSuccess() {
        return Code.isSuccess(this.code);
    }

    /**
     * 判断当前结果是否失败
     * @return true-失败, false-成功
     */
    public boolean isFailure() {
        return Code.isFailure(this.code);
    }
}
