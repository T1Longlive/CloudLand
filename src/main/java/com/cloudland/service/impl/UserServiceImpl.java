package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.Interceptor.MyInterceptor;
import com.cloudland.config.StorageProperties;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.UserMapper;
import com.cloudland.pojo.User;
import com.cloudland.pojo.dto.UpdatePasswordDTO;
import com.cloudland.service.IUserService;
import com.cloudland.util.EmailUtils;
import com.cloudland.util.FileUtil;
import com.cloudland.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;
    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private HttpServletRequest request;
    @Resource
    private EmailUtils emailUtils;
    @Resource
    private LandMapper landMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private StorageProperties storageProperties;

    /** 万能验证码开关（code.master-enabled）：开启后登录/注册/找回密码均可用万能码通过校验 */
    @Value("${code.master-enabled:false}")
    private boolean masterCodeEnabled;

    /** 万能验证码值（code.master-value），默认 000000 */
    @Value("${code.master-value:000000}")
    private String masterCode;

    @Override
    public Result loginUser(User user, HttpServletRequest request) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        boolean remember = Boolean.parseBoolean(request.getHeader("remember"));
        boolean frond = Boolean.parseBoolean(request.getHeader("frond"));

        if (request.getHeader("token") != null && user.getPassword() == null) {
            log.info("token登录");
            String token = request.getHeader("token");
            Claims claims;
            try {
                claims = jwtUtils.parseJWT(token);
            } catch (Exception e) {
                log.error("后端令牌验证失败: {}", e.getMessage());
                return new Result(Code.TOKEN_ERR, null, Msg.TOKEN_ERR);
            }

            Integer userId = claims.get("id", Integer.class);
            String phone = claims.get("phone", String.class);
            User result = userId == null ? null : userMapper.selectById(userId);
            if (result != null && Objects.equals(phone, result.getPhone())) {
                if (result.getStatus() == 0) {
                    return new Result(Code.STATUS_ERR, null, Msg.STATUS_ERR);
                }
                if (frond || result.getPower() == 1 || result.getPower() == 2) {
                    String updatedToken = jwtUtils.generateJwt(buildTokenClaims(result), remember);
                    result.setPassword(null);
                    return new Result(Code.LOGIN_OK, result, updatedToken);
                }
                return new Result(Code.POWER_ERR, null, Msg.POWER_ERR);
            }
            return new Result(Code.LOGIN_RETURN, null, Msg.LOGIN_RETURN);
        }

        log.info("密码登录");
        String code = request.getHeader("code");
        if (code != null) {
            String mail = selectByEmail(user);
            if (mail == null) {
                // 手机号不存在：验证码通道查不到邮箱，走账号不存在分支
                // （原实现会执行 redisTemplate.delete(null) 抛异常返回 500）
                return new Result(Code.PHONE_NO_EXIST, null, Msg.PHONE_NO_EXIST);
            }
            if (!CodeCheck(mail, code)) {
                return new Result(Code.CODE_ERR, null, Msg.CODE_ERR);
            }
            redisTemplate.delete(mail);
        }

        queryWrapper.eq("phone", user.getPhone());
        User result = userMapper.selectOne(queryWrapper);
        if (result == null) {
            return new Result(Code.PHONE_NO_EXIST, null, Msg.PHONE_NO_EXIST);
        }
        if (!passwordEncoder.matches(user.getPassword(), result.getPassword())) {
            return new Result(Code.PASSWORD_ERR, null, Msg.PASSWORD_ERR);
        }
        if (result.getStatus() == 0) {
            return new Result(Code.STATUS_ERR, null, Msg.STATUS_ERR);
        }

        String jwt = jwtUtils.generateJwt(buildTokenClaims(result), remember);
        if (frond || result.getPower() == 1 || result.getPower() == 2) {
            result.setPassword(null);
            return new Result(Code.LOGIN_OK, result, jwt);
        }
        return new Result(Code.POWER_ERR, null, Msg.POWER_ERR);
    }

    @Override
    public Result save(MultipartFile file, User user) {
        String code = request.getHeader("code");
        if (code != null) {
            String mail = user.getMail();
            if (!CodeCheck(mail, code)) {
                return new Result(Code.CODE_ERR, null, Msg.CODE_ERR);
            }
            redisTemplate.delete(user.getMail());
        }
        user.setId(null);
        // 防提权：注册只能是普通客户（power=0，激活状态，无欠款），
        // 客户端传入的 power/status/debt 一律忽略（曾可传 power=2 直接注册为管理员）
        user.setPower(0);
        user.setStatus(1);
        user.setDebt(0.0);
        if (!isUserNotRegistered(user)) {
            return new Result(Code.PHONE_EXIST, null, Msg.PHONE_EXIST);
        }
        if (!isMailNotRegistered(user)) {
            return new Result(Code.MAIL_EXIST, null, Msg.MAIL_EXIST);
        }

        if (file != null) {
            List<Object> objects = fileUtil.defineDirectory(null, null, null, file, null);
            user.setImg((String) objects.get(0));
        } else {
            user.setImg("basic.png");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return new Result(Code.REGISTER_OK, null, Msg.REGISTER_OK);
    }

    @Override
    public Result delete(Integer[] ids) {
        for (Integer id : ids) {
            User user = userMapper.selectById(id);
            String folderPath = storageProperties.getUserIconPath(user.getImg());
            fileUtil.deleteFolder(new File(folderPath));
        }
        // 解除土地的员工外键引用，避免外键约束报错
        landMapper.update(null, Wrappers.<com.cloudland.pojo.Land>lambdaUpdate()
                .in(com.cloudland.pojo.Land::getEmployeeId, Arrays.asList(ids))
                .set(com.cloudland.pojo.Land::getEmployeeId, null));
        userMapper.deleteBatchIds(Arrays.asList(ids));
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    @Override
    public Result update(MultipartFile userIcon, User user, HttpServletRequest request) {
        // 越权防护：普通用户只能修改自己的资料，且禁止变更 power/status/debt（防提权）
        Integer loginId = (Integer) request.getAttribute(MyInterceptor.ATTR_USER_ID);
        Integer loginPower = (Integer) request.getAttribute(MyInterceptor.ATTR_USER_POWER);
        if (loginId != null && (loginPower == null || loginPower < 1)) {
            User current = userMapper.selectById(loginId);
            if (current == null) {
                return new Result(Code.POWER_ERR, null, Msg.POWER_ERR);
            }
            user.setId(loginId);
            user.setPower(current.getPower());
            user.setStatus(current.getStatus());
            user.setDebt(current.getDebt());
        }
        if (!isUserNotRegistered(user)) {
            return new Result(Code.PHONE_EXIST, null, Msg.PHONE_EXIST);
        }
        if (!isMailNotRegistered(user)) {
            return new Result(Code.MAIL_EXIST, null, Msg.MAIL_EXIST);
        }

        if (userIcon != null) {
            String folderPath = storageProperties.getUserIconPath(user.getImg());
            fileUtil.deleteFolder(new File(folderPath));
            List<Object> objects = fileUtil.defineDirectory(null, null, null, userIcon, null);
            user.setImg((String) objects.get(0));
        }

        boolean frond = request.getHeader("frond") != null && Boolean.parseBoolean(request.getHeader("frond"));
        if (frond) {
            // 前台场景：密码仅用于身份确认（改绑定信息时必须验当前密码），
            // 修改密码一律走专用端点 PUT /user/password，本接口不再处理前台改密
            User result = userMapper.selectById(user.getId());
            if (result == null) {
                return new Result(Code.PHONE_NO_EXIST, null, Msg.PHONE_NO_EXIST);
            }
            if (user.getPassword() == null || !passwordEncoder.matches(user.getPassword(), result.getPassword())) {
                return new Result(Code.PASSWORD_ERR, null, Msg.PASSWORD_ERR);
            }
            if (Boolean.parseBoolean(request.getHeader("contact"))) {
                // 改绑定：手机/邮箱至少一项变化才允许提交
                if (Objects.equals(user.getMail(), result.getMail()) && Objects.equals(user.getPhone(), result.getPhone())) {
                    return new Result(Code.UPDATE_SAME, null, Msg.UPDATE_SAME);
                }
            }
            // 前台请求保留原密码哈希（不通过本接口改密）
            user.setPassword(result.getPassword());
        } else if (user.getPassword() != null) {
            // 后台管理：管理员可直接设置新密码（重置密码）
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userMapper.updateById(user);
        return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
    }

    @Override
    public Result updatePassword(UpdatePasswordDTO dto, HttpServletRequest request) {
        // 用户 ID 一律取登录态，不信任请求体（防越权改他人密码）
        Integer loginId = (Integer) request.getAttribute(MyInterceptor.ATTR_USER_ID);
        if (loginId == null) {
            return new Result(Code.POWER_ERR, null, Msg.POWER_ERR);
        }
        User result = userMapper.selectById(loginId);
        if (result == null) {
            return new Result(Code.PHONE_NO_EXIST, null, Msg.PHONE_NO_EXIST);
        }
        if (dto.getOldPassword() == null || !passwordEncoder.matches(dto.getOldPassword(), result.getPassword())) {
            return new Result(Code.PASSWORD_ERR, null, Msg.PASSWORD_ERR);
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().equals(dto.getOldPassword())) {
            return new Result(Code.PASSWORD_SAME, null, Msg.PASSWORD_SAME);
        }
        // 只更新密码字段，避免携带其他字段误改资料
        User update = new User();
        update.setId(loginId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
        return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
    }

    @Override
    public IPage<User> selectPage(int pageNum, int pageSize, User user) {
        return userMapper.selectByCondition(new Page<>(pageNum, pageSize), user);
    }

    private Boolean isUserNotRegistered(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", user.getPhone());
        User result = userMapper.selectOne(queryWrapper);
        if (result == null) {
            return true;
        }
        if (user.getId() != null) {
            return Objects.equals(user.getId(), result.getId());
        }
        return false;
    }

    private Boolean isMailNotRegistered(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mail", user.getMail());
        User result = userMapper.selectOne(queryWrapper);
        if (result == null) {
            return true;
        }
        if (user.getId() != null) {
            return Objects.equals(user.getId(), result.getId());
        }
        return false;
    }

    @Override
    public Result sendOutEmail(User user) {
        if (user.getMail() == null) {
            user.setMail(this.selectByEmail(user));
            if (user.getMail() == null) {
                return new Result(Code.SEND_MAIL_ERR, null, Msg.SEND_MAIL_ERR);
            }
        }
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        redisTemplate.opsForValue().set(user.getMail(), code, 5, TimeUnit.MINUTES);
        String subject = "云用地团队";
        String msg = "<h4>欢迎您使用云用地</h4>" + "<span>您的验证码为: </span>"
                + "<span style='color:#105147;text-decoration: underline'>" + code + "</span><p>5分钟内有效</p>";
        return emailUtils.sendEmail(user.getMail(), subject, msg);
    }

    @Override
    public Result forgetPassword(User user, String path) {
        if (user.getId() != null) {
            if (request.getHeader("forgetPassword") != null && Boolean.parseBoolean(request.getHeader("forgetPassword"))) {
                String password = userMapper.selectById(user.getId()).getPassword();
                if (passwordEncoder.matches(user.getPassword(), password)) {
                    return new Result(Code.PASSWORD_SAME, null, Msg.PASSWORD_SAME);
                }
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userMapper.updateById(user);
                return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
            }

            user = userMapper.selectById(user.getId());
            if (isCodeValid(user.getPhone(), path)) {
                redisTemplate.delete(user.getPhone());
                return new Result(Code.UPDATE_OK, null, Msg.UPDATE_OK);
            }
            return new Result(Code.UPDATE_ERR, null, Msg.UPDATE_ERR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", user.getPhone());
        user = userMapper.selectOne(queryWrapper);
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        redisTemplate.opsForValue().set(user.getPhone(), code, 5, TimeUnit.MINUTES);
        String subject = "云用地团队";
        String msg = "<h4>欢迎您使用云用地</h4>" + "<span>您的密码重置链接为：</span>"
                + "<a href=" + path + code + "-" + user.getId()
                + " style='color:#105147;text-decoration: underline'>" + path + code + "-" + user.getId()
                + "</a><p>链接5分钟内有效（注：该链接只生效一次！）</p>";
        return emailUtils.sendEmail(user.getMail(), subject, msg);
    }

    public String selectByEmail(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", user.getPhone());
        User result = userMapper.selectOne(queryWrapper);
        return result == null ? null : result.getMail();
    }

    public Boolean CodeCheck(String key, String code) {
        return isCodeValid(key, code);
    }

    /**
     * 统一验证码校验：优先比对 Redis 中的真实验证码；
     * 若万能验证码开关开启（code.master-enabled），masterCode（默认 000000）可直接通过。
     * 覆盖场景：登录（邮箱验证码）、注册（邮箱验证码）、找回密码（重置链接中的验证码）。
     */
    private boolean isCodeValid(String redisKey, String code) {
        if (masterCodeEnabled && masterCode != null && masterCode.equals(code)) {
            log.warn("万能验证码通过校验（测试便利功能，生产环境请关闭）: key={}", redisKey);
            return true;
        }
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        return Objects.equals(code, redisCode);
    }

    private Map<String, Object> buildTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("phone", user.getPhone());
        claims.put("username", user.getUsername());
        claims.put("power", user.getPower());
        claims.put("debt", user.getDebt());
        return claims;
    }
}
