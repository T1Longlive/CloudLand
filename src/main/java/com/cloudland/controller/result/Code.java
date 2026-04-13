package com.cloudland.controller.result;

/**
 * 业务状态码常量
 *
 * 设计原则：
 * 1. 使用10000+范围，避开HTTP标准状态码（100-599）
 * 2. 按业务模块分段管理
 * 3. 添加通用SUCCESS/FAILURE标识
 *
 * 状态码分段：
 * - 0/-1: 通用成功/失败标识
 * - 10000-10999: CRUD操作
 * - 20000-20999: 认证授权
 * - 30000-30999: 邮件通知
 * - 40000-40999: 业务特定
 */
public class Code {

    // ==================== 通用状态码 ====================
    /**
     * 通用成功标识
     */
    public static final Integer SUCCESS = 0;

    /**
     * 通用失败标识
     */
    public static final Integer FAILURE = -1;

    // ==================== CRUD操作状态码 (10000-10999) ====================
    /**
     * 添加成功
     */
    public static final Integer ADD_OK = 10001;

    /**
     * 删除成功
     */
    public static final Integer DELETE_OK = 10002;

    /**
     * 修改成功
     */
    public static final Integer UPDATE_OK = 10003;

    /**
     * 查询成功
     */
    public static final Integer SELECT_OK = 10004;

    /**
     * 添加失败
     */
    public static final Integer ADD_ERR = 10005;

    /**
     * 删除失败
     */
    public static final Integer DELETE_ERR = 10006;

    /**
     * 修改失败
     */
    public static final Integer UPDATE_ERR = 10007;

    /**
     * 查询失败
     */
    public static final Integer SELECT_ERR = 10008;

    // ==================== 认证授权状态码 (20000-20999) ====================
    /**
     * 账号不存在
     */
    public static final Integer PHONE_NO_EXIST = 20001;

    /**
     * 密码错误
     */
    public static final Integer PASSWORD_ERR = 20002;

    /**
     * 令牌过期
     */
    public static final Integer TOKEN_ERR = 20003;

    /**
     * 账号被禁用
     */
    public static final Integer STATUS_ERR = 20004;

    /**
     * 登录成功
     */
    public static final Integer LOGIN_OK = 20005;

    /**
     * 注册成功
     */
    public static final Integer REGISTER_OK = 20006;

    /**
     * 该手机号已注册
     */
    public static final Integer PHONE_EXIST = 20007;

    /**
     * 权限不足
     */
    public static final Integer POWER_ERR = 20008;

    /**
     * 请重新登陆
     */
    public static final Integer LOGIN_RETURN = 20009;

    /**
     * 密码相同
     */
    public static final Integer PASSWORD_SAME = 20010;

    /**
     * 绑定信息相同
     */
    public static final Integer UPDATE_SAME = 20011;

    // ==================== 邮件通知状态码 (30000-30999) ====================
    /**
     * 邮件发送成功
     */
    public static final Integer SEND_MAIL_OK = 30001;

    /**
     * 邮件发送失败
     */
    public static final Integer SEND_MAIL_ERR = 30002;

    /**
     * 验证码错误
     */
    public static final Integer CODE_ERR = 30003;

    /**
     * 该邮箱已注册
     */
    public static final Integer MAIL_EXIST = 30004;

    // ==================== 工具方法 ====================
    /**
     * 判断是否为成功状态码
     * @param code 状态码
     * @return true-成功, false-失败
     */
    public static boolean isSuccess(Integer code) {
        if (code == null) {
            return false;
        }
        // SUCCESS 或 以OK结尾的状态码都视为成功
        return code.equals(SUCCESS) ||
               code.equals(ADD_OK) ||
               code.equals(DELETE_OK) ||
               code.equals(UPDATE_OK) ||
               code.equals(SELECT_OK) ||
               code.equals(LOGIN_OK) ||
               code.equals(REGISTER_OK) ||
               code.equals(SEND_MAIL_OK);
    }

    /**
     * 判断是否为失败状态码
     * @param code 状态码
     * @return true-失败, false-成功
     */
    public static boolean isFailure(Integer code) {
        return !isSuccess(code);
    }
}
