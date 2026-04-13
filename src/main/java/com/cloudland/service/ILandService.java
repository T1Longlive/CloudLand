package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Land;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.vo.LandVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
public interface ILandService extends IService<Land> {
    Result save(MultipartFile[] files, MultipartFile[] imageFiles, Land land);
    Result delete(Integer[] ids);
    Result update(MultipartFile[] landFiles, MultipartFile[] imageFiles, Land land);
    IPage<LandVO> selectPage(int pageNum, int pageSize, Land land);

    LandVO selectById(Integer id);
}
