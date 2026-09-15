package com.cloudland.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JwtUtils} 的纯单元测试。
 *
 * <p>⚠️ 不启动 Spring 上下文：{@code JwtUtils} 用的是**字段注入**
 * （{@code @Value} 打在 private 字段上，没有构造器），所以这里用
 * {@link ReflectionTestUtils} 塞值，照样不需要容器。
 *
 * <p>顺带暴露一个设计味道：**字段注入让这个类没法直接 new 出来测**。
 * 改成构造器注入（把参数写在构造器上，Spring 一样能注入）就能省掉反射，
 * 见 pm-system 的 {@code JwtUtil} —— 那也是同样的功能、更好的可测性。
 *
 * <p>测的是**安全性质**，不只是"能签发能解析"：
 * 过期、签名被篡改、换密钥伪造 —— 这三种都必须被拒绝。
 * 这是"能编译但逻辑写错"最危险的区域：一旦校验被绕过，整个鉴权就形同虚设。
 */
class JwtUtilsTest {

    /**
     * jjwt 0.9.1 的 {@code signWith(alg, String)} 会把字符串**按 base64 解码**当密钥用，
     * 所以这里用合法的 base64 串（解码后 32 字节，满足 HS256 的密钥长度）。
     */
    private static final String KEY = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3OA==";
    private static final String OTHER_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    private static final Long SEVEN_DAYS_MS = 604800000L;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = newJwtUtils(KEY, SEVEN_DAYS_MS, SEVEN_DAYS_MS);
    }

    /** 反射构造一个 JwtUtils（因为没有构造器可用） */
    private static JwtUtils newJwtUtils(String signKey, Long expire, Long week) {
        JwtUtils u = new JwtUtils();
        ReflectionTestUtils.setField(u, "signKey", signKey);
        ReflectionTestUtils.setField(u, "expire", expire);
        ReflectionTestUtils.setField(u, "week", week);
        return u;
    }

    private static Map<String, Object> claims() {
        Map<String, Object> c = new HashMap<String, Object>();
        c.put("id", 42);
        c.put("username", "alice");
        return c;
    }

    @Test
    @DisplayName("签发后能解析回来，自定义声明原样保留")
    void roundTrip() {
        String jwt = jwtUtils.generateJwt(claims(), Boolean.FALSE);

        Claims parsed = jwtUtils.parseJWT(jwt);

        assertThat(parsed.get("id")).isEqualTo(42);
        assertThat(parsed.get("username")).isEqualTo("alice");
    }

    @Test
    @DisplayName("remember=true 的令牌比 remember=false 活得更久（分别取 week / expire 配置）")
    void rememberSelectsDuration() {
        // 当前 application.yml 里 expire 与 week 的默认值相同，直接用默认值测不出区别，
        // 所以这里显式设成两个不同的值，验证「选了哪一个」。
        long oneMinute = 60_000L;
        long sevenDays = SEVEN_DAYS_MS;
        JwtUtils u = newJwtUtils(KEY, oneMinute, sevenDays);

        long shortLived = remainingMs(u, u.generateJwt(claims(), Boolean.FALSE));
        long longLived = remainingMs(u, u.generateJwt(claims(), Boolean.TRUE));

        assertThat(shortLived).isBetween(50_000L, 61_000L);          // 约 1 分钟
        assertThat(longLived).isBetween(604_000_000L, 604_800_000L); // 约 7 天
    }

    @Test
    @DisplayName("已过期的令牌必须被拒绝")
    void expiredRejected() {
        JwtUtils u = newJwtUtils(KEY, -1000L, -1000L); // 有效期 -1 秒 ⇒ 签发即已过期

        String jwt = u.generateJwt(claims(), Boolean.FALSE);

        assertThatThrownBy(() -> u.parseJWT(jwt))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("用别的密钥签发的令牌必须被拒绝（防伪造）")
    void tokenFromAnotherKeyRejected() {
        JwtUtils attacker = newJwtUtils(OTHER_KEY, SEVEN_DAYS_MS, SEVEN_DAYS_MS);

        String forged = attacker.generateJwt(claims(), Boolean.FALSE);

        assertThatThrownBy(() -> jwtUtils.parseJWT(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("被改动过的令牌必须被拒绝（防篡改）")
    void tamperedTokenRejected() {
        String jwt = jwtUtils.generateJwt(claims(), Boolean.FALSE);

        // 改掉签名段的第一个字符，签名随即失配
        int lastDot = jwt.lastIndexOf('.');
        char original = jwt.charAt(lastDot + 1);
        char flipped = (original == 'A') ? 'B' : 'A';
        String tampered = jwt.substring(0, lastDot + 1) + flipped + jwt.substring(lastDot + 2);

        assertThatThrownBy(() -> jwtUtils.parseJWT(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("乱码不是令牌，必须被拒绝（且抛的是 JwtException 而不是别的异常）")
    void malformedTokenRejected() {
        assertThatThrownBy(() -> jwtUtils.parseJWT("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    /** 令牌剩余有效期（毫秒） */
    private static long remainingMs(JwtUtils u, String jwt) {
        return u.parseJWT(jwt).getExpiration().getTime() - System.currentTimeMillis();
    }
}
