package com.cloudland.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.config.StorageProperties;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Product;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.pojo.vo.ProductVO;
import com.cloudland.service.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author longlive
 * @since 2023-11-04
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {
    @Resource
    private FileUtil fileUtil;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private StorageProperties storageProperties;

    @Override
    public Result save(MultipartFile file, Product product) {
        List<Object> objects = fileUtil.defineDirectory(null, null, null, null,file);
        product.setImg((String) objects.get(0));
        productMapper.insert(product);
        return new  Result(Code.ADD_OK,null, Msg.ADD_OK);
    }

    @Override
    public Result delete(Integer[] ids) {
        for (Integer id : ids) {
            Product product = productMapper.selectById(id);
            String folderPath = storageProperties.getProductPath(product.getImg());
            fileUtil.deleteFolder(new File(folderPath));
        }
        productMapper.deleteBatchIds(Arrays.asList(ids));
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    @Override
    public Result update(MultipartFile productImg, Product product) {
        if (productImg != null) {
            String folderPath = storageProperties.getProductPath(product.getImg());
            fileUtil.deleteFolder(new File(folderPath));
            List<Object> objects = fileUtil.defineDirectory(null, null, null, null,productImg);
            product.setImg((String) objects.get(0));
        }
        productMapper.updateById(product);
        return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
    }

    @Override
    public Page<ProductVO> selectPage(int pageNum, int pageSize, Product product) {
        return productMapper.selectByCondition(new Page<>(pageNum, pageSize), product);
    }

    @Override
    public ProductVO selectById(Integer id) {
        Product product = new Product();
        product.setId(id);
        Page<ProductVO> productVOPage = this.selectPage(1, 1, product);
        return productVOPage.getRecords().get(0);
    }
}
