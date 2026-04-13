package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Msg;
import com.cloudland.mapper.MsgMapper;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.service.IMsgService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
@Service
public class MsgServiceImpl extends ServiceImpl<MsgMapper, Msg> implements IMsgService {
    @Resource
    private MsgMapper msgMapper;

    @Override
    public IPage<Msg> selectPage(int pageNum, int pageSize, Msg msg) {
        return msgMapper.selectByCondition(new Page<>(pageNum, pageSize), msg);
    }
}
