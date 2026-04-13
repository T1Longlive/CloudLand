/**
 * 业务状态码常量
 * 与后端 Code.java 保持一致
 *
 * 使用方式：
 * import { Code } from '@/constants/code'
 * if (res.data.code === Code.SUCCESS) { ... }
 */

export const Code = {
  // ==================== 通用状态码 ====================
  /** 通用成功标识 */
  SUCCESS: 0,
  /** 通用失败标识 */
  FAILURE: -1,

  // ==================== CRUD操作状态码 (10000-10999) ====================
  /** 添加成功 */
  ADD_OK: 10001,
  /** 删除成功 */
  DELETE_OK: 10002,
  /** 修改成功 */
  UPDATE_OK: 10003,
  /** 查询成功 */
  SELECT_OK: 10004,
  /** 添加失败 */
  ADD_ERR: 10005,
  /** 删除失败 */
  DELETE_ERR: 10006,
  /** 修改失败 */
  UPDATE_ERR: 10007,
  /** 查询失败 */
  SELECT_ERR: 10008,

  // ==================== 认证授权状态码 (20000-20999) ====================
  /** 账号不存在 */
  PHONE_NO_EXIST: 20001,
  /** 密码错误 */
  PASSWORD_ERR: 20002,
  /** 令牌过期 */
  TOKEN_ERR: 20003,
  /** 账号被禁用 */
  STATUS_ERR: 20004,
  /** 登录成功 */
  LOGIN_OK: 20005,
  /** 注册成功 */
  REGISTER_OK: 20006,
  /** 该手机号已注册 */
  PHONE_EXIST: 20007,
  /** 权限不足 */
  POWER_ERR: 20008,
  /** 请重新登陆 */
  LOGIN_RETURN: 20009,
  /** 密码相同 */
  PASSWORD_SAME: 20010,
  /** 绑定信息相同 */
  UPDATE_SAME: 20011,

  // ==================== 邮件通知状态码 (30000-30999) ====================
  /** 邮件发送成功 */
  SEND_MAIL_OK: 30001,
  /** 邮件发送失败 */
  SEND_MAIL_ERR: 30002,
  /** 验证码错误 */
  CODE_ERR: 30003,
  /** 该邮箱已注册 */
  MAIL_EXIST: 30004,
}

/**
 * 判断是否为成功状态码
 * @param {number} code 状态码
 * @returns {boolean} true-成功, false-失败
 */
export function isSuccess(code) {
  if (code === null || code === undefined) {
    return false
  }
  return code === Code.SUCCESS ||
    code === Code.ADD_OK ||
    code === Code.DELETE_OK ||
    code === Code.UPDATE_OK ||
    code === Code.SELECT_OK ||
    code === Code.LOGIN_OK ||
    code === Code.REGISTER_OK ||
    code === Code.SEND_MAIL_OK
}

/**
 * 判断是否为失败状态码
 * @param {number} code 状态码
 * @returns {boolean} true-失败, false-成功
 */
export function isFailure(code) {
  return !isSuccess(code)
}

/**
 * 获取状态码对应的消息（用于调试）
 * @param {number} code 状态码
 * @returns {string} 状态码说明
 */
export function getCodeMessage(code) {
  const codeMap = {
    [Code.SUCCESS]: '操作成功',
    [Code.FAILURE]: '操作失败',
    [Code.ADD_OK]: '添加成功',
    [Code.DELETE_OK]: '删除成功',
    [Code.UPDATE_OK]: '修改成功',
    [Code.SELECT_OK]: '查询成功',
    [Code.ADD_ERR]: '添加失败',
    [Code.DELETE_ERR]: '删除失败',
    [Code.UPDATE_ERR]: '修改失败',
    [Code.SELECT_ERR]: '查询失败',
    [Code.PHONE_NO_EXIST]: '账号不存在',
    [Code.PASSWORD_ERR]: '密码错误',
    [Code.TOKEN_ERR]: '令牌过期',
    [Code.STATUS_ERR]: '账号被禁用',
    [Code.LOGIN_OK]: '登录成功',
    [Code.REGISTER_OK]: '注册成功',
    [Code.PHONE_EXIST]: '该手机号已注册',
    [Code.POWER_ERR]: '权限不足',
    [Code.LOGIN_RETURN]: '请重新登陆',
    [Code.PASSWORD_SAME]: '密码相同',
    [Code.UPDATE_SAME]: '绑定信息相同',
    [Code.SEND_MAIL_OK]: '邮件发送成功',
    [Code.SEND_MAIL_ERR]: '邮件发送失败',
    [Code.CODE_ERR]: '验证码错误',
    [Code.MAIL_EXIST]: '该邮箱已注册',
  }
  return codeMap[code] || `未知状态码: ${code}`
}

export default Code
