package com.cloudland.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 工具单元测试。
 *
 * <p>为什么值得单测：JwtUtils 是整个鉴权体系的地基。
 * 它一旦出问题，表现是"所有人都登录不上"或者更糟——"签名校验形同虚设"，
 * 而后者在功能测试里完全测不出来（正常 token 照样能用）。
 *
 * <p>这里不启动 Spring 容器：JwtUtils 的三个字段是 {@code @Value} 注入的，
 * 用 {@code ReflectionTestUtils} 直接赋值即可。
 * 纯单元测试跑 50ms，比启一个 ApplicationContext 快两个数量级 ——
 * 能在 CI 里每个 PR 都跑，才叫"门禁"。
 */
class JwtUtilsTest {

    /**
     * 测试用签名密钥（Base64）。
     *
     * <p>jjwt 0.9.x 的 String 形式密钥会被当作 Base64 解码，
     * 所以这里用合法的 Base64，避免测试因为"密钥格式"这种无关原因而红。
     */
    private static final String SIGN_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rz";

    /** 普通登录有效期：60 秒（测试里不需要真的 7 天）。 */
    private static final long EXPIRE_MS = 60_000L;

    /** "记住我"有效期：7 天。 */
    private static final long WEEK_MS = 7 * 24 * 60 * 60 * 1000L;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "signKey", SIGN_KEY);
        ReflectionTestUtils.setField(jwtUtils, "expire", EXPIRE_MS);
        ReflectionTestUtils.setField(jwtUtils, "week", WEEK_MS);
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 692);
        claims.put("phone", "19900000001");
        claims.put("power", 0);
        return claims;
    }

    @Test
    @DisplayName("生成后能解析回来，且声明内容一致")
    void roundTrip() {
        String jwt = jwtUtils.generateJwt(claims(), false);

        Claims parsed = jwtUtils.parseJWT(jwt);

        assertThat(parsed.get("id", Integer.class)).isEqualTo(692);
        assertThat(parsed.get("phone", String.class)).isEqualTo("19900000001");
        assertThat(parsed.get("power", Integer.class)).isEqualTo(0);
    }

    @Test
    @DisplayName("生成的是标准三段式 JWT")
    void tokenHasThreeSegments() {
        String jwt = jwtUtils.generateJwt(claims(), false);
        assertThat(jwt.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("普通登录有效期等于 jwt.expire")
    void expiryFollowsExpireConfig() {
        long before = System.currentTimeMillis();
        Claims parsed = jwtUtils.parseJWT(jwtUtils.generateJwt(claims(), false));
        long ttl = parsed.getExpiration().getTime() - before;

        // 允许 5 秒误差，避免 CI 机器调度抖动导致偶发失败
        assertThat(ttl).isBetween(EXPIRE_MS - 5_000L, EXPIRE_MS + 5_000L);
    }

    @Test
    @DisplayName("remember=true 时有效期变成 jwt.week")
    void rememberMeUsesWeekConfig() {
        long before = System.currentTimeMillis();
        Claims parsed = jwtUtils.parseJWT(jwtUtils.generateJwt(claims(), true));
        long ttl = parsed.getExpiration().getTime() - before;

        assertThat(ttl).isBetween(WEEK_MS - 5_000L, WEEK_MS + 5_000L);
    }

    @Test
    @DisplayName("签名被篡改的 token 必须解析失败，不能只验结构")
    void tamperedTokenIsRejected() {
        String jwt = jwtUtils.generateJwt(claims(), false);
        // 改签名段中间字符：base64url 尾字符的低位比特不参与解码，
        // 改尾字符可能解码出相同字节（签名未变），导致用例概率性假绿/假红
        String[] parts = jwt.split("\\.");
        char[] signature = parts[2].toCharArray();
        int mid = signature.length / 2;
        signature[mid] = signature[mid] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + new String(signature);

        assertThatThrownBy(() -> jwtUtils.parseJWT(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("换一个密钥签发的 token 必须被拒绝")
    void tokenSignedByOtherKeyIsRejected() {
        JwtUtils other = new JwtUtils();
        ReflectionTestUtils.setField(other, "signKey", "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdHM=");
        ReflectionTestUtils.setField(other, "expire", EXPIRE_MS);
        ReflectionTestUtils.setField(other, "week", WEEK_MS);

        String foreignToken = other.generateJwt(claims(), false);

        assertThatThrownBy(() -> jwtUtils.parseJWT(foreignToken))
                .as("用别的密钥签发的 token 绝不能被接受，否则等于没有鉴权")
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期 token 抛 ExpiredJwtException")
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtils, "expire", -1_000L);  // 签发即过期
        String expired = jwtUtils.generateJwt(claims(), false);

        assertThatThrownBy(() -> jwtUtils.parseJWT(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("垃圾字符串不能静默通过")
    void garbageStringIsRejected() {
        assertThatThrownBy(() -> jwtUtils.parseJWT("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("空声明也能签发，不抛异常")
    void emptyClaimsAreAllowed() {
        String jwt = jwtUtils.generateJwt(new HashMap<>(), false);
        assertThat(jwtUtils.parseJWT(jwt).get("id")).isNull();
    }

    @Test
    @DisplayName("过期时间必须严格大于当前时间（防配置成 0 导致永久有效）")
    void expirationIsInFuture() {
        Claims parsed = jwtUtils.parseJWT(jwtUtils.generateJwt(claims(), false));
        assertThat(parsed.getExpiration()).isAfter(new Date());
    }
}
