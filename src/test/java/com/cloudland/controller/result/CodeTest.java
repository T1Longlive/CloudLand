package com.cloudland.controller.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 码表契约测试。
 *
 * <p>为什么这层必须有单元测试：
 * {@link Code#isSuccess(Integer)} 是一份**手写的白名单**，而不是"以 OK 结尾"之类的规则判断。
 * 这意味着每新增一个业务码，都必须记得同步改这个白名单，否则新接口会返回"成功码"却被判定为失败。
 * 这种漏洞集成测试很难发现（因为前端直接看 code 数值），只有对着码表做穷举才能守住。
 *
 * <p>断言方式刻意分成两种：
 * <ul>
 *   <li>穷举具体数值 —— 防止白名单被人手误改；</li>
 *   <li>反射扫描全类字段 —— 防止有人加了新常量却忘了加进白名单。</li>
 * </ul>
 */
class CodeTest {

    /** 与 Code.isSuccess 的实现保持一致的成功码集合。 */
    private static final Integer[] SUCCESS_CODES = {
            Code.SUCCESS, Code.ADD_OK, Code.DELETE_OK, Code.UPDATE_OK,
            Code.SELECT_OK, Code.LOGIN_OK, Code.REGISTER_OK, Code.SEND_MAIL_OK
    };

    @ParameterizedTest(name = "code={0} 应判定为成功")
    @ValueSource(ints = {0, 10001, 10002, 10003, 10004, 20005, 20006, 30001})
    @DisplayName("白名单内的码一律成功")
    void successCodes(int code) {
        assertThat(Code.isSuccess(code)).isTrue();
        assertThat(Code.isFailure(code)).isFalse();
    }

    @ParameterizedTest(name = "code={0} 应判定为失败")
    @ValueSource(ints = {
            -1,
            10005, 10006, 10007, 10008,
            20001, 20002, 20003, 20004, 20007, 20008, 20009, 20010, 20011,
            30002, 30003, 30004
    })
    @DisplayName("白名单外的码一律失败，包括各种 ERR 码")
    void failureCodes(int code) {
        assertThat(Code.isSuccess(code)).isFalse();
        assertThat(Code.isFailure(code)).isTrue();
    }

    @Test
    @DisplayName("null 必须判为失败，不能抛空指针")
    void nullIsFailure() {
        // 后端大量地方会把 Result.code 置空（如 401 时根本没有 body），
        // isSuccess(null) 如果抛 NPE，会把"未登录"变成 500。
        assertThat(Code.isSuccess(null)).isFalse();
        assertThat(Code.isFailure(null)).isTrue();
    }

    @Test
    @DisplayName("反射扫描：所有 *_OK 常量都在白名单内，其余都在白名单外")
    void whitelistCoversAllConstants() {
        List<Integer> missing = new ArrayList<>();   // 名字像成功，实际判失败
        List<Integer> wrong = new ArrayList<>();     // 名字不像成功，实际判成功

        for (Field field : Code.class.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod)) {
                continue;
            }
            if (field.getType() != Integer.class) {
                continue;
            }
            Integer value;
            try {
                value = (Integer) field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("无法读取常量 " + field.getName(), e);
            }
            String name = field.getName();
            boolean nameLooksLikeSuccess = "SUCCESS".equals(name) || name.endsWith("_OK");

            if (nameLooksLikeSuccess && !Code.isSuccess(value)) {
                missing.add(value);
            }
            if (!nameLooksLikeSuccess && Code.isSuccess(value)) {
                wrong.add(value);
            }
        }

        assertThat(missing)
                .as("这些常量名字带 OK/SUCCESS，但 Code.isSuccess 没把它们算成功，前端会当成失败")
                .isEmpty();
        assertThat(wrong)
                .as("这些常量不在成功命名约定内，却被判为成功，可能是白名单写错")
                .isEmpty();
    }

    @Test
    @DisplayName("成功码集合恰好 8 个，新增成功码必须同步更新本测试")
    void successCodeCountIsStable() {
        assertThat(SUCCESS_CODES).hasSize(8);
    }
}
