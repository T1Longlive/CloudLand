package com.cloudland.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloudland.config.StorageProperties;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Msg;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.CloudLandFileMapper;
import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.mapper.TrolleyMapper;
import com.cloudland.mapper.UserMapper;
import com.cloudland.pojo.CloudLandFile;
import com.cloudland.pojo.Product;
import com.cloudland.pojo.Trolley;
import com.cloudland.pojo.User;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.pojo.vo.TrolleyVo;
import com.cloudland.service.IUserService;
import com.cloudland.util.EmailUtils;
import com.cloudland.util.FileUtil;
import com.cloudland.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
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
    private TrolleyMapper trolleyMapper;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;
    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private HttpServletRequest request;
    @Resource
    private CloudLandFileMapper cloudLandFileMapper;
    @Resource
    private EmailUtils emailUtils;
    @Resource
    private LandMapper landMapper;
    @Resource
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private StorageProperties storageProperties;

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
        userMapper.deleteBatchIds(Arrays.asList(ids));
        return new Result(Code.DELETE_OK, null, Msg.DELETE_ERR);
    }

    @Override
    public Result update(MultipartFile userIcon, User user, HttpServletRequest request) {
        if (!isUserNotRegistered(user)) {
            return new Result(Code.PHONE_EXIST, null, Msg.PHONE_EXIST);
        }
        if (!isMailNotRegistered(user)) {
            return new Result(Code.MAIL_EXIST, null, Msg.MAIL_EXIST);
        }

        if (request.getHeader("frond") != null && Boolean.parseBoolean(request.getHeader("frond"))) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", user.getId());
            User result = userMapper.selectOne(queryWrapper);
            if (!passwordEncoder.matches(user.getPassword(), result.getPassword())) {
                return new Result(Code.PASSWORD_ERR, null, Msg.PASSWORD_ERR);
            }
        }

        if (userIcon != null) {
            String folderPath = storageProperties.getUserIconPath(user.getImg());
            fileUtil.deleteFolder(new File(folderPath));
            List<Object> objects = fileUtil.defineDirectory(null, null, null, userIcon, null);
            user.setImg((String) objects.get(0));
        }

        if (user.getPassword() != null) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", user.getId());
            User result = userMapper.selectOne(queryWrapper);
            if (request.getHeader("frond") != null && Boolean.parseBoolean(request.getHeader("frond"))) {
                if (!Boolean.parseBoolean(request.getHeader("contact"))) {
                    if (!passwordEncoder.matches(user.getPassword(), result.getPassword())) {
                        return new Result(Code.PASSWORD_ERR, null, Msg.PASSWORD_ERR);
                    }
                    if (passwordEncoder.matches(user.getDetailedAddress(), result.getPassword())) {
                        return new Result(Code.PASSWORD_SAME, null, Msg.PASSWORD_SAME);
                    }
                    user.setDetailedAddress(result.getDetailedAddress());
                    user.setPassword(passwordEncoder.encode(user.getDetailedAddress()));
                } else {
                    if (Objects.equals(user.getMail(), result.getMail()) && Objects.equals(user.getPhone(), result.getPhone())) {
                        return new Result(Code.UPDATE_SAME, null, Msg.UPDATE_SAME);
                    }
                    user.setPassword(result.getPassword());
                }
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }

        userMapper.updateById(user);
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
            String redisCode = redisTemplate.opsForValue().get(user.getPhone());
            if (redisCode != null && Objects.equals(path, redisCode)) {
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

    public Boolean CodeCheck(String mail, String code) {
        String redisCode = redisTemplate.opsForValue().get(mail);
        return Objects.equals(code, redisCode);
    }

    public Result selectTrolley(User user) {
        QueryWrapper<Trolley> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("u_id", user.getId());
        queryWrapper.orderByDesc("id");
        List<Trolley> trolleys = trolleyMapper.selectList(queryWrapper);
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

    public Result deleteTrolley(Integer id) {
        trolleyMapper.deleteById(id);
        return new Result(Code.DELETE_OK, null, Msg.DELETE_OK);
    }

    public Result addTrolley(Trolley trolley) {
        trolleyMapper.insert(trolley);
        return new Result(Code.ADD_OK, null, Msg.ADD_OK);
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
