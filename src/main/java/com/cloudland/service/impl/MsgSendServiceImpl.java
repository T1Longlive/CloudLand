package com.cloudland.service.impl;

import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.MsgSend;
import com.cloudland.mapper.MsgSendMapper;
import com.cloudland.service.IMsgSendService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.util.EmailUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
@Service
public class MsgSendServiceImpl extends ServiceImpl<MsgSendMapper, MsgSend> implements IMsgSendService {
    @Resource
    private EmailUtils emailUtils;
    @Override
    public Result sendOutEmail(String mail,String pushMsg) throws Exception {
        String subject = "云用地团队";
        String msg = "<h4>云用地订阅消息</h4>" + "<span style='color:#105147;text-decoration: underline'>" + pushMsg + "</span>";
        return emailUtils.sendEmail(mail, subject, msg);
    }
}
