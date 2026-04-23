package com.cloudland.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.service.IOrder2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
@Slf4j
public class AlipayController {

    @Resource
    private AlipayClient alipayClient;
    @Resource
    private IOrder2Service orderService;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;
    @Value("${alipay.returnUrl}")
    private String returnUrl;
    @Value("${alipay.publicKey}")
    private String alipayPublicKey;

    @PostMapping("/pay")
    public Result pay(@RequestParam("orderIds") Integer[] orderIds,
                      @RequestParam("totalAmount") String totalAmount,
                      @RequestParam("userId") Integer userId) throws AlipayApiException {
        String outTradeNo = "CLD" + System.currentTimeMillis() + userId;

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"云用地订单支付\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        // 存储交易号与订单ID的映射（Redis或临时存储）
        orderService.saveTradeMapping(outTradeNo, orderIds);

        String form = alipayClient.pageExecute(request).getBody();
        return new Result(Code.ADD_OK, form, Msg.ADD_OK);
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        log.info("支付宝回调参数: {}", params);
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            log.info("验签结果: {}, trade_status: {}", signVerified, params.get("trade_status"));
            if (signVerified && "TRADE_SUCCESS".equals(params.get("trade_status"))) {
                String outTradeNo = params.get("out_trade_no");
                orderService.handlePaySuccess(outTradeNo);
                return "success";
            }
        } catch (Exception e) {
            log.error("支付宝回调处理失败: {}", e.getMessage(), e);
        }
        return "failure";
    }
}
