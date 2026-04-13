package com.cloudland.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.pojo.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudland.pojo.vo.ProductVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author longlive
 * @since 2023-11-04
 */
public interface ProductMapper extends BaseMapper<Product> {
    Page<ProductVO> selectByCondition(@Param("page")Page<Product> page, @Param("product") Product product);
}
