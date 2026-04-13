package com.cloudland.Interceptor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cloudland.mapper.UserMapper;
import com.cloudland.pojo.User;
import com.cloudland.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Slf4j
@Component
public class MyInterceptor implements HandlerInterceptor {
    private static final String AUTH_HEADER = "token";
    private static final String LOGIN_URL_KEYWORD = "login";
    private static final String REGISTER_URL_KEYWORD = "register";
    private static final String FORGET_PASSWORD = "forgetPassword";
    private static final String CODE_URL_KEYWORD = "code";

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, @NotNull HttpServletResponse resp, @NotNull Object handler) {
        String url = req.getRequestURL().toString();
        log.info("请求的URL: {}", url);

        if (isLoginOrRegister(url)) {
            log.info("登录或注册操作，开始放行...");
            return true;
        }

        String token = req.getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return handleUnauthorizedRequest("请求缺少令牌");
        }

        try {
            Claims claims = jwtUtils.parseJWT(token);
            Integer userId = claims.get("id", Integer.class);
            if (userId == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return handleUnauthorizedRequest("令牌缺少用户标识");
            }

            User result = userMapper.selectById(userId);
            if (result == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return handleUnauthorizedRequest("用户不存在");
            }

            if (result.getStatus() == 0) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return handleUnauthorizedRequest("账号被禁用");
            }

            Integer tokenPower = claims.get("power", Integer.class);
            if (tokenPower != null && !Objects.equals(tokenPower, result.getPower())) {
                resp.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                return handleUnauthorizedRequest("账号权限已变更");
            }
        } catch (Exception e) {
            log.error("令牌验证失败: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return handleUnauthorizedRequest("令牌验证失败");
        }

        log.info("令牌验证通过，放行");
        return true;
    }

    private boolean isLoginOrRegister(String url) {
        return url.contains(LOGIN_URL_KEYWORD)
                || url.contains(REGISTER_URL_KEYWORD)
                || url.contains(CODE_URL_KEYWORD)
                || url.contains(FORGET_PASSWORD);
    }

    private boolean handleUnauthorizedRequest(String errorMessage) {
        log.error("Unauthorized request: {}", errorMessage);
        return false;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
    }
}
