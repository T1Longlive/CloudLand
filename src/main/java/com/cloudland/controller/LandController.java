package com.cloudland.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;

import com.cloudland.pojo.Land;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.service.ILandService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
@RestController
@RequestMapping("/land")
public class LandController {
    @Resource
    private ILandService landService;

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        LandVO land = landService.selectById(id);
        return new Result(Code.SELECT_OK, land, Msg.SELECT_OK);
    }

    @PostMapping
    public Result addLand(@RequestParam("landFiles") MultipartFile[] landFiles, @RequestParam("imageFiles") MultipartFile[] imageFiles, @RequestParam(value = "land") String landStr) {
        Land land = JSON.parseObject(landStr, Land.class);
        land.setId(null);
        return landService.save(landFiles, imageFiles, land);
    }

    @DeleteMapping
    public Result delete(@RequestBody Integer[] ids) {
        return landService.delete(ids);
    }

    @PutMapping
    public Result update(@RequestParam(value = "landFiles", required = false) MultipartFile[] landFiles, @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles, @RequestParam(value = "land") String landStr) {
        Land land = JSON.parseObject(landStr, Land.class);
        return landService.update(landFiles, imageFiles, land);
    }

    @PostMapping("/page")
    public Result selectPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "land") String landStr) {
        //把前端的land条件字符串转成Java对象
        Land land = JSON.parseObject(landStr, Land.class);
        //把当前页和每页条数、查询条件land传入服务层
        IPage<LandVO> landIPage = landService.selectPage(pageNum, pageSize, land);
        Integer code = landIPage == null ? Code.SELECT_ERR : Code.SELECT_OK;
        String msg = landIPage != null ? Msg.SELECT_OK : Msg.SELECT_ERR;
        assert landIPage != null;
        return new Result(code, landIPage, msg);
    }
}
