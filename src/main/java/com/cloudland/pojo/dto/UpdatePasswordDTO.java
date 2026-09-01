package com.cloudland.pojo.dto;

import lombok.Data;

/**
 * 前台自助修改密码请求体。
 * 用户 ID 不由请求体传入，统一从登录态（MyInterceptor 注入）获取，防止越权改他人密码。
 */
@Data
public class UpdatePasswordDTO {

    /** 当前（旧）密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
