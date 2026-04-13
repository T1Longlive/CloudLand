package com.cloudland.util;

import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

@Component
public class EmailUtils {

    @Resource
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    public Result sendEmail(String to, String subject, String context) {
        System.out.println(to);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(context, true);
            javaMailSender.send(mimeMessage);
            System.out.println("发送成功");
            return new Result(Code.SEND_MAIL_OK, null, Msg.SEND_MAIL_OK);
        } catch (Exception e) {
            System.out.println("发送失败");
            e.printStackTrace();
            return new Result(Code.SEND_MAIL_ERR, null, Msg.SEND_MAIL_ERR);
        }
    }
}
