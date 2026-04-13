package com.cloudland.service;

import com.cloudland.controller.result.Result;
import com.cloudland.pojo.MsgSend;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
public interface IMsgSendService extends IService<MsgSend> {
    Result sendOutEmail(String mail,String msg) throws Exception;
}
