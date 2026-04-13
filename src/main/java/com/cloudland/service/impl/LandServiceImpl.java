package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.config.StorageProperties;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.CloudLandFileMapper;
import com.cloudland.mapper.LandMapper;
import com.cloudland.pojo.CloudLandFile;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.service.ILandService;
import com.cloudland.pojo.Land;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
@Service
@Slf4j
public class LandServiceImpl extends ServiceImpl<LandMapper, Land> implements ILandService {
    @Resource
    LandMapper landMapper;
    @Resource
    CloudLandFileMapper cloudLandFileMapper;
    @Resource
    FileUtil fileUtil;
    @Resource
    private StorageProperties storageProperties;

    @Override
    public Result save(MultipartFile[] landFiles, MultipartFile[] imageFiles, Land land) {
        //land.id是空说明是添加操作,否则是修改操作
        if (land.getId() == null) {
            //添加到数据库,取得自增的id
            landMapper.insert(land);
        } else {
            //修改数据库的land
            landMapper.updateById(land);
        }
        //创建Land商品文件夹,录入资源文件到本地,dataLists集合内有:图片路径集合,资料文件压缩包路径字符串两个内容
        List<Object> dataLists = fileUtil.defineDirectory(land.getId(), landFiles, imageFiles, null,null);

        //创建一个云用地文件表pojo对象
        CloudLandFile cloudLandFile = new CloudLandFile();

        //设置云用地文件表pojo对象数据
        cloudLandFile.setLandId(land.getId());

        //设置压缩包路径
        //判断第一个元素是否是字符类型
        if (dataLists.get(0) instanceof String) {
            cloudLandFile.setPath((String) dataLists.get(0));
            cloudLandFile.setType(1);
            //添加压缩包路径数据
         log.info("-------------" + cloudLandFile);
            cloudLandFileMapper.insert(cloudLandFile);
        }

        //设置存储图片文件路径
        //判断第二个元素是否是集合类型
        if (dataLists.get(1) instanceof List<?>) {
            List<?> images = (List<?>) dataLists.get(1);
            for (Object imgPath : images) {
                cloudLandFile.setId(null);
                cloudLandFile.setPath((String) imgPath);
                cloudLandFile.setType(0);
                //添加单个图片路径数据
                cloudLandFileMapper.insert(cloudLandFile);
            }
        }
        return new Result(Code.ADD_OK, land, Msg.ADD_OK);
    }

    @Override
    public Result delete(Integer[] ids) {
        landMapper.deleteBatchIds(Arrays.asList(ids));
        for (Integer id : ids) {
            String folderPath = storageProperties.getLandDir(id);
            fileUtil.deleteFolder(new File(folderPath));
        }
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    @Override
    public Result update(MultipartFile[] landFiles, MultipartFile[] imageFiles, Land land) {
        if (landFiles == null && imageFiles == null) {
            landMapper.updateById(land);
            return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
        }
        if (landFiles != null) {
            QueryWrapper<CloudLandFile> landWrapper = Wrappers.query();
            landWrapper.eq("land_id", land.getId());
            landWrapper.eq("type", 1);
            CloudLandFile landFile = cloudLandFileMapper.selectOne(landWrapper);
            String folderPath = landFile == null
                    ? storageProperties.getLandZipPath(land.getId(), "云用地_" + land.getId() + ".zip")
                    : storageProperties.getLandZipPath(land.getId(), landFile.getPath());
            fileUtil.deleteFolder(new File(folderPath));
            cloudLandFileMapper.delete(landWrapper);
        }
        if (imageFiles != null) {
            String folderPath = storageProperties.getLandImagesDir(land.getId());
            QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
            fileUtil.deleteFolder(new File(folderPath));
            imgWrapper.eq("land_id", land.getId());
            imgWrapper.eq("type", 0);
            cloudLandFileMapper.delete(imgWrapper);
        }
        save(landFiles, imageFiles, land);
        return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
    }

    public Page<LandVO> selectPage(int pageNum, int pageSize, Land land) {
        //查询分页
        Page<LandVO> landVOPage = landMapper.selectByCondition(new Page<>(pageNum, pageSize), land);
        //分页查询结果提取landVO对象集合
        List<LandVO> landVOS = landVOPage.getRecords();
        //补充landVO对象集合的图片文件和用地资料存储地址信息
        for (LandVO landVO : landVOS) {
            //-------------------------图片地址查询、封装、赋值
            // 创建一个查询条件的Wrapper对象
            QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
            //指定对应land的文件所以加上id条件
            imgWrapper.eq("land_id", landVO.getId());
            //type=0表示图片文件
            imgWrapper.eq("type", 0);
            //得到图片文件地址结果集
            List<CloudLandFile> imgList = cloudLandFileMapper.selectList(imgWrapper);
            //把结果集的地址再遍历加工修改
            for (CloudLandFile img : imgList) {
                img.setPath("Land_" + landVO.getId() + "/Images/" + img.getPath());
            }
            //录入图片文件地址信息到landVO.imageFiles[]
            landVO.setImageFiles(imgList);
            //-------------------------资料地址查询、封装、赋值
            QueryWrapper<CloudLandFile> landWrapper = Wrappers.query();
            landWrapper.eq("land_id", landVO.getId());
            landWrapper.eq("type", 1);
            CloudLandFile landFileZIP = cloudLandFileMapper.selectOne(landWrapper);
            landFileZIP.setPath("Land_" + landVO.getId() + "/" + landFileZIP.getPath());
            //录入资料文件地址信息到landVO.landFiles
            landVO.setLandFiles(landFileZIP);
        }
        //把更新后的landVOS集合重新放入分页查询结果集里面
        landVOPage.setRecords(landVOS);
        return landVOPage;
    }

    @Override
    public LandVO selectById(Integer id) {
        Land land = new Land();
        land.setId(id);
        Page<LandVO> landVOPage = this.selectPage(1, 1, land);
        return landVOPage.getRecords().get(0);
    }
}
