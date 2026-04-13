package com.cloudland.util;

import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.Order2Mapper;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Order2;
import com.cloudland.pojo.Product;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Component
public class OrderClear {
    @Resource
    private Order2Mapper order2Mapper;
    @Resource
    private LandMapper landMapper;
    @Resource
    private ProductMapper productMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeTask() {
        List<Order2> order2s = order2Mapper.selectList(null);
        for (Order2 order : order2s) {
            if (order.getStatus() == 0) {
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
                order2Mapper.deleteById(order);
            }
        }
    }
}
