package com.cloudland.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.pojo.Order2;
import com.cloudland.pojo.Trolley;
import com.cloudland.pojo.User;
import com.cloudland.service.ITrolleyService;
import com.cloudland.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    UserServiceImpl userService;
    @Resource
    ITrolleyService trolleyService;
    @PostMapping("/login")
    public Result login(@RequestBody  User user,HttpServletRequest request) {
        return userService.loginUser(user,request);
    }

    @PostMapping("/register")
    public Result register(@RequestParam(value = "userIcon",required = false) MultipartFile userIcon, @RequestParam(value = "user") String userStr) {
        User user = JSON.parseObject(userStr, User.class);
        return  userService.save(userIcon, user);
    }

    @DeleteMapping
    public Result delete(@RequestBody Integer[] ids) {
        return userService.delete(ids);
    }

    @PutMapping
    public Result update(@RequestParam(value = "userIcon",required = false) MultipartFile userIcon, @RequestParam(value = "user") String userStr,HttpServletRequest request) {
        User user = JSON.parseObject(userStr, User.class);
        return userService.update(userIcon, user,request);
    }

    @PostMapping("/page")
    public Result selectPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "user") String userStr) {
        //把前端的land条件字符串转成Java对象
        User user = JSON.parseObject(userStr, User.class);
        //把当前页和每页条数、查询条件land传入服务层
        IPage<User> employeeIPage = userService.selectPage(pageNum, pageSize, user);
        return new Result(Code.SELECT_OK, employeeIPage, Msg.SELECT_OK);
    }

    @PostMapping("/employee")
    public Result selectAllId() {
        Wrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("power", 2)
                .select("id", "username");
        List<User> users = userService.list(queryWrapper);
        return new Result(Code.SELECT_OK, users, Msg.SELECT_OK);
    }

    @PostMapping("/id")
    public Result selectById(@RequestBody Long  id) {
        Wrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("id", id);
        User user = userService.getOne(queryWrapper);
        user.setPassword(null);
        return new Result(Code.SELECT_OK, user, Msg.SELECT_OK);
    }

    @PostMapping("/code")
    public Result sendCode(@RequestBody  User user){
        return userService.sendOutEmail(user);
    }


    @PostMapping("/forgetPassword")
    public Result forgetPassword(     @RequestParam(value = "path") String path,
                                      @RequestParam(value = "user") String userStr){
        User user = JSON.parseObject(userStr, User.class);
        return userService.forgetPassword(user,path);
    }

    @PostMapping("/trolley")
    public Result myTrolley(@RequestBody  User user){
        return trolleyService.selectTrolley(user);
    }
    @PostMapping("/addTrolley")
    public Result addTrolley(@RequestBody Trolley trolley){
        return trolleyService.addTrolley(trolley);
    }
    @DeleteMapping("/trolley/{id}")
    public Result deleteTrolley(@PathVariable Integer id) {
        return trolleyService.deleteTrolley(id);
    }
}
