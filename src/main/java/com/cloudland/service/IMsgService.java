package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Msg;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloudland.pojo.vo.LandVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
public interface IMsgService extends IService<Msg> {
    IPage<Msg> selectPage(int pageNum, int pageSize, Msg msg);
}
