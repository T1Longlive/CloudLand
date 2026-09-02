package com.cloudland.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.cloudland.Interceptor.MyInterceptor;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.service.IOrder2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
                      @RequestParam(value = "totalAmount", required = false) String clientAmount,
                      @RequestParam(value = "userId", required = false) Integer clientUserId,
                      HttpServletRequest request) throws AlipayApiException {
        try {
            // 金额服务端重算：忽略前端传入的 totalAmount（防篡改）
            // 计价规则与 selectOrder 组装 OrderVO.price 一致：土地订单=land.price，产品订单=product.price×num
            BigDecimal totalAmount = orderService.calcTotalAmount(orderIds);
            if (clientAmount != null && log.isInfoEnabled()) {
                log.info("支付金额服务端校验: server={}, client={}", totalAmount, clientAmount);
            }
            // 交易号使用登录用户 ID（前端传入的 userId 仅作历史兼容，不再信任）
            Object currentUserId = request.getAttribute(MyInterceptor.ATTR_USER_ID);
            String userIdPart = currentUserId != null ? String.valueOf(currentUserId) : String.valueOf(System.nanoTime() % 1000);
            String outTradeNo = "CLD" + System.currentTimeMillis() + userIdPart;

            AlipayTradePagePayRequest request2 = new AlipayTradePagePayRequest();
            request2.setNotifyUrl(notifyUrl);
            request2.setReturnUrl(returnUrl);
            request2.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\","
                    + "\"total_amount\":\"" + totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + "\","
                    + "\"subject\":\"云用地订单支付\","
                    + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

            // 存储交易号与订单ID的映射（Redis或临时存储），内部含归属校验
            orderService.saveTradeMapping(outTradeNo, orderIds);

            String form = alipayClient.pageExecute(request2).getBody();
            return new Result(Code.ADD_OK, form, Msg.ADD_OK);
        } catch (RuntimeException e) {
            // 业务规则违规（订单不存在/已支付/归属校验失败）返回业务码而非 500
            log.warn("支付请求被拒绝: {}", e.getMessage());
            return new Result(Code.ADD_ERR, null, e.getMessage());
        }
    }

    @GetMapping("/return")
    public Result alipayReturn(@RequestParam("out_trade_no") String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                orderService.handlePaySuccess(outTradeNo);
                return new Result(Code.ADD_OK, null, Msg.ADD_OK);
            }
            log.warn("支付宝交易查询失败: {}, {}", response.getSubCode(), response.getSubMsg());
            return new Result(Code.ADD_ERR, null, "支付未完成");
        } catch (AlipayApiException e) {
            log.error("查询支付宝交易状态失败: {}", e.getMessage(), e);
            return new Result(Code.ADD_ERR, null, "查询支付状态失败");
        }
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
