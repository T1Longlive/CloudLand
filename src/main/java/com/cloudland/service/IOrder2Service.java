package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Order2;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.OrderVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
}
