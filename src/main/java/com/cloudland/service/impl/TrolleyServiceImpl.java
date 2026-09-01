package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.Interceptor.MyInterceptor;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.CloudLandFileMapper;
import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.mapper.TrolleyMapper;
import com.cloudland.pojo.CloudLandFile;
import com.cloudland.pojo.Product;
import com.cloudland.pojo.Trolley;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.pojo.vo.TrolleyVo;
import com.cloudland.service.ITrolleyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrolleyServiceImpl extends ServiceImpl<TrolleyMapper, Trolley> implements ITrolleyService {

    @Resource
    private LandMapper landMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private CloudLandFileMapper cloudLandFileMapper;
    @Resource
    private HttpServletRequest request;

    @Override
    public Result selectTrolley(User user) {
        QueryWrapper<Trolley> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("u_id", user.getId());
        queryWrapper.orderByDesc("id");
        List<Trolley> trolleys = this.baseMapper.selectList(queryWrapper);
        List<TrolleyVo> trolleyVos = new ArrayList<>();
        for (Trolley trolley : trolleys) {
            TrolleyVo trolleyVo = new TrolleyVo();
            trolleyVo.setId(trolley.getId());
            trolleyVo.setPId(trolley.getPId());
            trolleyVo.setNum(trolley.getNum());
            trolleyVo.setUId(trolley.getUId());
            if (trolley.getNum() == -1) {
                LandVO landVO = landMapper.selectById(trolley.getPId());
                QueryWrapper<CloudLandFile> imgWrapper = Wrappers.query();
                imgWrapper.eq("land_id", landVO.getId());
                imgWrapper.eq("type", 0);
                List<CloudLandFile> imgList = cloudLandFileMapper.selectList(imgWrapper);
                imgList.get(0).setPath("Land_" + landVO.getId() + "/Images/" + imgList.get(0).getPath());
                landVO.setImageFiles(imgList);
                trolleyVo.setPrice(landVO.getPrice());
                trolleyVo.setProductName(landVO.getLandName());
                trolleyVo.setStatus(landVO.getStatus());
                trolleyVo.setImg(imgList.get(0).getPath());
            } else {
                Product product = productMapper.selectById(trolley.getPId());
                trolleyVo.setPrice(product.getPrice());
                trolleyVo.setProductName(product.getProductName());
                trolleyVo.setImg(product.getImg());
                trolleyVo.setProductNum(product.getNum());
                trolleyVo.setStatus(product.getStatus());
            }
            trolleyVos.add(trolleyVo);
        }
        return new Result(Code.SELECT_OK, trolleyVos, Msg.SELECT_OK);
    }

    @Override
    public Result deleteTrolley(Integer id) {
        // 归属校验：普通用户只能删除自己的购物车条目；员工/管理员不受限
        Trolley trolley = this.baseMapper.selectById(id);
        if (trolley != null) {
            Integer uid = (Integer) request.getAttribute(MyInterceptor.ATTR_USER_ID);
            Integer power = (Integer) request.getAttribute(MyInterceptor.ATTR_USER_POWER);
            boolean isStaff = power != null && power >= 1;
            if (!isStaff && (uid == null || !uid.equals(trolley.getUId()))) {
                return new Result(Code.POWER_ERR, null, Msg.POWER_ERR);
            }
        }
        this.baseMapper.deleteById(id);
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    @Override
    public Result addTrolley(Trolley trolley) {
        this.baseMapper.insert(trolley);
        return new Result(Code.ADD_OK, null, Msg.ADD_OK);
    }
}
