package com.cloudland.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Msg;
import com.cloudland.pojo.MsgSend;
import com.cloudland.service.IMsgSendService;
import com.cloudland.service.IMsgService;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
@RestController
@RequestMapping("/msg")
public class MsgController {
    @Resource
    private IMsgService msgService;
    @Resource
    private IMsgSendService msgSendService;


    @PostMapping
    public Result msg(@RequestBody Msg msg) {
        QueryWrapper<Msg> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", msg.getPhone());
        List<Msg> msgs = msgService.list(queryWrapper);
        if (msgs.size()>9){
            return new Result(Code.ADD_ERR, msg, "你的留言已超过10条,暂时无法留言......");
        }else {
            LocalDateTime currentDateTime = LocalDateTime.now();
            msg.setSendTime(currentDateTime);
            msgService.save(msg);
            return new Result(Code.ADD_OK, msg, "发送成功!");
        }
    }

    @PostMapping("/mail")
    public Result subscribe(@RequestBody MsgSend msgSend) {
        QueryWrapper<MsgSend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mail", msgSend.getMail());
        MsgSend msgSend1 = msgSendService.getOne(queryWrapper);
        if (msgSend1==null){
            msgSendService.save(msgSend);
            return new Result(Code.ADD_OK, null, "订阅成功!");
        }else {
            return new Result(Code.ADD_ERR, null, "你已经订阅了!");
        }
    }


    @PostMapping("/page")
    public Result selectPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "msg") String msgStr) {

        //把前端的land条件字符串转成Java对象
        Msg userMsg = JSON.parseObject(msgStr, Msg.class);
        //把当前页和每页条数、查询条件land传入服务层
        IPage<Msg> msgIPage = msgService.selectPage(pageNum, pageSize, userMsg);
        Integer code = msgIPage == null ? Code.SELECT_ERR : Code.SELECT_OK;
        String msg = msgIPage != null ? com.cloudland.controller.result.Msg.SELECT_OK : com.cloudland.controller.result.Msg.SELECT_ERR;
        assert msgIPage != null;
        return new Result(code, msgIPage, msg);
    }
    @PostMapping("/push")
    public Result push( @RequestParam(value = "pushMsg") String pushMsg) throws Exception {
        List<MsgSend> pushList = msgSendService.list();
        for(MsgSend msg:pushList){
            msgSendService.sendOutEmail(msg.getMail(),pushMsg);
        }
        return new Result(Code.SEND_MAIL_OK, null, "推送成功");
    }


}
