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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class MyInterceptor implements HandlerInterceptor {
    /** request attribute 名：当前登录用户 ID */
    public static final String ATTR_USER_ID = "currentUserId";
    /** request attribute 名：当前登录用户权限（0 客户 / 1 员工 / 2 管理员） */
    public static final String ATTR_USER_POWER = "currentUserPower";
    /** 403 响应头：原因标识（status=账号禁用 / power=权限不足），供前端区分处理 */
    public static final String HEADER_FORBIDDEN_REASON = "X-Forbidden-Reason";

    private static final String AUTH_HEADER = "token";

    /**
     * 后台管理接口（"METHOD servletPath" 精确匹配，servletPath 不含 context-path），
     * 要求当前登录用户 power >= 1（员工/管理员）。
     * 注意：前后台共用的接口（如 PUT /user、DELETE /order/{id}）不在此列，
     * 由 Service 层按归属做细粒度校验。
     */
    private static final Set<String> ADMIN_RULES = new HashSet<>(Arrays.asList(
            // 用户管理（含 /user/id 按人查资料：前台无调用，含手机号/邮箱等敏感信息，仅限员工/管理员）
            "POST /user/page",
            "POST /user/employee",
            "POST /user/id",
            "DELETE /user",
            // 土地管理（/land/page、/land/{id} 为公开查询，不在此列）
            "POST /land",
            "PUT /land",
            "DELETE /land",
            // 产品管理（/product/page、/product/{id} 为公开查询，不在此列）
            "POST /product",
            "PUT /product",
            "DELETE /product",
            // 订单管理（前台 DELETE /order/{id}、POST /order、POST /order/order 为共享接口）
            "POST /order/page",
            "PUT /order",
            "POST /order/download",
            // 消息管理（POST /msg、POST /msg/mail 为公开留言/订阅）
            "POST /msg/page",
            "POST /msg/push"
    ));

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, @NotNull HttpServletResponse resp, @NotNull Object handler) {
        String url = req.getRequestURL().toString();

        String token = req.getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("请求缺少令牌: {}", url);
            return false;
        }

        Claims claims;
        try {
            claims = jwtUtils.parseJWT(token);
        } catch (Exception e) {
            log.warn("令牌验证失败: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Integer userId = claims.get("id", Integer.class);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("令牌缺少用户标识: {}", url);
            return false;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("用户不存在: userId={}", userId);
            return false;
        }

        if (user.getStatus() == 0) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setHeader(HEADER_FORBIDDEN_REASON, "status");
            log.warn("账号被禁用: userId={}", userId);
            return false;
        }

        Integer tokenPower = claims.get("power", Integer.class);
        if (tokenPower != null && !Objects.equals(tokenPower, user.getPower())) {
            resp.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            log.warn("账号权限已变更: userId={}", userId);
            return false;
        }

        // 注入当前登录用户上下文，供 Controller/Service 做归属校验
        req.setAttribute(ATTR_USER_ID, user.getId());
        req.setAttribute(ATTR_USER_POWER, user.getPower());

        // 后台管理接口授权：要求 power >= 1
        String rule = req.getMethod() + " " + req.getServletPath();
        if (ADMIN_RULES.contains(rule) && (user.getPower() == null || user.getPower() < 1)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setHeader(HEADER_FORBIDDEN_REASON, "power");
            log.warn("权限不足，拒绝访问后台接口: {} userId={}", rule, userId);
            return false;
        }

        log.info("令牌验证通过: userId={} power={} {}", userId, user.getPower(), url);
        return true;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
    }
}
