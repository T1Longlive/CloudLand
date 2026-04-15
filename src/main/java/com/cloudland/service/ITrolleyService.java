package com.cloudland.service;

import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Trolley;
import com.cloudland.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2024-02-19
 */
public interface ITrolleyService extends IService<Trolley> {

    Result selectTrolley(User user);

    Result deleteTrolley(Integer id);

    Result addTrolley(Trolley trolley);
}
