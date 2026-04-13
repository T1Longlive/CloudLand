package com.cloudland.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
public interface IUserService extends IService<User> {
    Result loginUser(User user, HttpServletRequest request);
    Result save(MultipartFile file, User user);
    Result delete(Integer[] ids);
    Result update(MultipartFile userIcon, User user,HttpServletRequest request);
    IPage<User> selectPage(int pageNum, int pageSize, User user);
    Result sendOutEmail(User user) throws Exception;
    Result forgetPassword(User user,String path) throws Exception;

}
