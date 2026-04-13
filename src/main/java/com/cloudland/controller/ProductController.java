package com.cloudland.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Product;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.pojo.vo.ProductVO;
import com.cloudland.service.ILandService;
import com.cloudland.service.IProductService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author longlive
 * @since 2023-11-04
 */
@RestController
@RequestMapping("/product")
public class ProductController {
    @Resource
    private IProductService productService;

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        ProductVO land = productService.selectById(id);
        return new Result(Code.SELECT_OK, land, Msg.SELECT_OK);
    }

    @PostMapping
    public Result addProduct(@RequestParam("productImg") MultipartFile productImg,  @RequestParam(value = "product") String productStr) {
        Product product = JSON.parseObject(productStr, Product.class);
        product.setId(null);
        return productService.save(productImg, product);
    }

    @DeleteMapping
    public Result delete(@RequestBody Integer[] ids) {
        return productService.delete(ids);
    }

    @PutMapping
    public Result update(@RequestParam(value = "productImg", required = false) MultipartFile productImg,@RequestParam(value = "product") String productStr) {
        Product product = JSON.parseObject(productStr, Product.class);
        return productService.update(productImg, product);
    }

    @PostMapping("/page")
    public Result selectPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "product") String productStr) {

        //把前端的land条件字符串转成Java对象
        Product product = JSON.parseObject(productStr, Product.class);
        //把当前页和每页条数、查询条件land传入服务层
        IPage<ProductVO> productIPage = productService.selectPage(pageNum, pageSize, product);
        Integer code = productIPage == null ? Code.SELECT_ERR : Code.SELECT_OK;
        String msg = productIPage != null ? Msg.SELECT_OK : Msg.SELECT_ERR;
        assert productIPage != null;
        return new Result(code, productIPage, msg);
    }
}
