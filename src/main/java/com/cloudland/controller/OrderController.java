package com.cloudland.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Order2;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.OrderVO;
import com.cloudland.service.IOrder2Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author longlive
 * @since 2023-11-04
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    @Resource
    private IOrder2Service orderService;

    @PostMapping("/order")
    public Result myTrolley(@RequestBody User user){
        return orderService.selectOrder(user);
    }

    @PostMapping()
    public Result addOrder(@RequestBody Order2 order){
        return orderService.addOrder(order);
    }

    @PutMapping()
    public Result updateOrder(@RequestParam(value = "ids") Integer[] ids,
                              @RequestParam(value = "status") Integer status,
                              @RequestParam(value = "time") Boolean time){
        return orderService.updateOrder(ids, status, time);
    }

    @DeleteMapping("/{id}")
    public Result deleteOrder(@PathVariable Integer id) {
        return orderService.deleteOrder(id);
    }

    @PostMapping("/page")
    public Result selectPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "order") String orderStr) {
        //把前端的land条件字符串转成Java对象
        Order2 order = JSON.parseObject(orderStr, Order2.class);
        //把当前页和每页条数、查询条件land传入服务层
        IPage<OrderVO> OrderVOIPage = orderService.selectPage(pageNum, pageSize, order);
        Integer code = OrderVOIPage == null ? Code.SELECT_ERR : Code.SELECT_OK;
        String msg = OrderVOIPage != null ? Msg.SELECT_OK : Msg.SELECT_ERR;
        assert OrderVOIPage != null;
        return new Result(code, OrderVOIPage, msg);
    }

    @PostMapping("/download")
    public void getRank(HttpServletResponse response, @RequestParam(value = "order") String orderStr) throws IOException {
        Order2 order = JSON.parseObject(orderStr, Order2.class);
        orderService.getOrder(response,order);
    }
}
