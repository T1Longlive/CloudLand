package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Product;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.vo.ProductVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2023-11-04
 */
public interface IProductService extends IService<Product> {
    Result save(MultipartFile file, Product product);
    Result delete(Integer[] ids);
    Result update(MultipartFile file, Product product);
    IPage<ProductVO> selectPage(int pageNum, int pageSize, Product product);
    ProductVO selectById(Integer id);
}
