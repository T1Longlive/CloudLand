package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Order2;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.OrderVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2024-02-21
 */
public interface IOrder2Service extends IService<Order2> {
    IPage<OrderVO> selectPage(int pageNum, int pageSize, Order2 order2);

    Result addOrder(Order2 order);

    Result deleteOrder(Integer id);

    Result selectOrder(User user);

    Result updateOrder(Integer[] ids, Integer status, Boolean time);

    void getOrder(HttpServletResponse response,Order2 order) throws IOException;

    void saveTradeMapping(String outTradeNo, Integer[] orderIds);

    /**
     * 服务端按订单重算支付总金额（防前端金额篡改）：
     * 土地订单（num=-1）取 land.price，产品订单取 product.price × num；仅允许未支付订单参与。
     * 订单不存在或状态异常时抛出 RuntimeException。
     */
    BigDecimal calcTotalAmount(Integer[] orderIds);

    void handlePaySuccess(String outTradeNo);
}
