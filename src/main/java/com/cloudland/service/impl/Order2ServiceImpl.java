package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.CloudLandFileMapper;
import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.Order2Mapper;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.pojo.CloudLandFile;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Order2;
import com.cloudland.pojo.Product;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.pojo.vo.OrderVO;
import com.cloudland.service.IOrder2Service;
import com.cloudland.util.DownloadUtil;
import com.cloudland.util.OrderExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class Order2ServiceImpl extends ServiceImpl<Order2Mapper, Order2> implements IOrder2Service {
    @Resource
    private LandMapper landMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private Order2Mapper orderMapper;
    @Resource
    private CloudLandFileMapper cloudLandFileMapper;
    @Resource
    private OrderExporter orderExporter;
    @Resource
    private DownloadUtil downloadUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String TRADE_KEY_PREFIX = "alipay:trade:";

    @Override
    public IPage<OrderVO> selectPage(int pageNum, int pageSize, Order2 order) {
        List<OrderVO> orderVOS;
        Page<OrderVO> orderVOPage;
        if (order.getNum() != null && order.getNum() == -1) {
            orderVOPage = orderMapper.selectByCondition(new Page<>(pageNum, pageSize), order);
            orderVOS = orderVOPage.getRecords();
            for (OrderVO orderVO : orderVOS) {
                QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
                imgWrapper.eq("land_id", orderVO.getPId());
                imgWrapper.eq("type", 0);
                List<CloudLandFile> imgList = cloudLandFileMapper.selectList(imgWrapper);
                imgList.get(0).setPath("Land_" + orderVO.getPId() + "/Images/" + imgList.get(0).getPath());
                orderVO.setImg(imgList.get(0).getPath());
            }
        } else {
            orderVOPage = orderMapper.selectByCondition2(new Page<>(pageNum, pageSize), order);
        }
        return orderVOPage;
    }

    public Result selectOrder(User user) {
        QueryWrapper<Order2> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("u_id", user.getId());
        queryWrapper.eq("del", 0);
        queryWrapper.orderByDesc("id");
        List<Order2> order2s = orderMapper.selectList(queryWrapper);
        List<OrderVO> orderVOS = new ArrayList<>();
        for (Order2 order2 : order2s) {
            OrderVO orderVO = new OrderVO();
            orderVO.setId(order2.getId());
            orderVO.setPId(order2.getPId());
            orderVO.setNum(order2.getNum());
            orderVO.setUId(order2.getUId());
            orderVO.setCreateTime(order2.getCreateTime());
            orderVO.setPayTime(order2.getPayTime());
            orderVO.setStatus(order2.getStatus());
            if (orderVO.getNum() == -1) {
                LandVO landVO = landMapper.selectById(order2.getPId());
                if (landVO == null) continue;
                QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
                imgWrapper.eq("land_id", landVO.getId());
                imgWrapper.eq("type", 0);
                List<CloudLandFile> imgList = cloudLandFileMapper.selectList(imgWrapper);
                imgList.get(0).setPath("Land_" + landVO.getId() + "/Images/" + imgList.get(0).getPath());
                landVO.setImageFiles(imgList);
                orderVO.setPrice(landVO.getPrice());
                orderVO.setProductName(landVO.getLandName());
                orderVO.setImg(imgList.get(0).getPath());
            } else {
                Product product = productMapper.selectById(order2.getPId());
                if (product == null) continue;
                orderVO.setPrice(product.getPrice());
                orderVO.setProductName(product.getProductName());
                orderVO.setImg(product.getImg());
            }
            orderVOS.add(orderVO);
        }
        return new Result(Code.SELECT_OK, orderVOS, Msg.SELECT_OK);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateOrder(Integer[] ids, Integer status, Boolean time) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        for (Integer id : ids) {
            Order2 order = orderMapper.selectById(id);
            order.setStatus(status);
            if (Boolean.TRUE.equals(time)) {
                order.setPayTime(currentDateTime);
            }
            orderMapper.updateById(order);
        }
        return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
    }

    @Override
    public void getOrder(HttpServletResponse response, Order2 order) throws IOException {
        List<OrderVO> orderVOS;
        int type = 0;
        if (order.getNum() != null && order.getNum() == -1) {
            orderVOS = orderMapper.selectOrder(order);
            for (OrderVO orderVO : orderVOS) {
                QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
                imgWrapper.eq("land_id", orderVO.getPId());
                imgWrapper.eq("type", 0);
                List<CloudLandFile> imgList = cloudLandFileMapper.selectList(imgWrapper);
                imgList.get(0).setPath("Land_" + orderVO.getPId() + "/Images/" + imgList.get(0).getPath());
                orderVO.setImg(imgList.get(0).getPath());
                orderVO.setNum(1);
            }
        } else {
            orderVOS = orderMapper.selectOrder2(order);
            type = 1;
        }
        orderExporter.exportToExcel(orderVOS, type);
        String name = type == 0 ? "云用地_用地订单.xlsx" : "云用地_产品订单.xlsx";
        downloadUtil.getFile(name, response);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result deleteOrder(Integer id) {
        Order2 order = orderMapper.selectById(id);
        if (order.getPayTime() != null && order.getStatus() == 1) {
            order.setDel(1);
            orderMapper.updateById(order);
        } else {
            if (order.getNum() == -1) {
                Land land = new Land();
                land.setId(order.getPId());
                land.setStatus(1);
                landMapper.updateById(land);
            } else {
                Product product = productMapper.selectById(order.getPId());
                product.setId(order.getPId());
                product.setNum(product.getNum() + order.getNum());
                productMapper.updateById(product);
            }
            orderMapper.deleteById(order);
        }
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    @Override
    public void saveTradeMapping(String outTradeNo, Integer[] orderIds) {
        try {
            String json = new ObjectMapper().writeValueAsString(orderIds);
            stringRedisTemplate.opsForValue().set(TRADE_KEY_PREFIX + outTradeNo, json, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("保存交易映射失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(String outTradeNo) {
        String json = stringRedisTemplate.opsForValue().get(TRADE_KEY_PREFIX + outTradeNo);
        if (json == null) return;
        try {
            Integer[] orderIds = new ObjectMapper().readValue(json, Integer[].class);
            updateOrder(orderIds, 1, true);
            stringRedisTemplate.delete(TRADE_KEY_PREFIX + outTradeNo);
        } catch (Exception e) {
            throw new RuntimeException("处理支付成功回调失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result addOrder(Order2 order) {
        if (order.getNum() == -1) {
            LandVO landVO = landMapper.selectById(order.getPId());
            if (landVO.getStatus() == 0) {
                return new Result(Code.ADD_ERR, null, Msg.ADD_ERR);
            }
            Land land = new Land();
            land.setId(order.getPId());
            land.setStatus(0);
            landMapper.updateById(land);
        } else {
            Product product = new Product();
            Product product1 = productMapper.selectById(order.getPId());
            if (product1.getNum() < order.getNum() || product1.getStatus() == 0) {
                return new Result(Code.ADD_ERR, null, Msg.ADD_ERR);
            }
            product.setNum(product1.getNum() - order.getNum());
            product.setId(order.getPId());
            productMapper.updateById(product);
        }
        LocalDateTime currentDateTime = LocalDateTime.now();
        order.setCreateTime(currentDateTime);
        order.setStatus(0);
        order.setDel(0);
        orderMapper.insert(order);
        return new Result(Code.ADD_OK, null, Msg.ADD_OK);
    }
}
