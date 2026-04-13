package com.cloudland.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.pojo.Order2;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudland.pojo.vo.OrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author longlive
 * @since 2024-02-21
 */
public interface Order2Mapper extends BaseMapper<Order2> {
    Page<OrderVO> selectByCondition(@Param("page")Page<OrderVO> page, @Param("order") Order2 order);
    Page<OrderVO> selectByCondition2(@Param("page")Page<OrderVO> page, @Param("order") Order2 order);
    List<OrderVO> selectOrder(@Param("order") Order2 order);
    List<OrderVO> selectOrder2(@Param("order") Order2 order);
}
