package com.cloudland.controller.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应包装类的行为测试。
 *
 * <p>Result 是全项目 36 个接口的返回形状，它的字段语义一旦错位（比如把 token 塞进 data
 * 而不是 msg），所有接口和前端约定会一起错，但编译期完全看不出来。所以这里把约定钉死。
 */
class ResultTest {

    @Test
    @DisplayName("登录成功时 JWT 放在 msg 字段，而不是 data")
    void loginTokenLivesInMsg() {
        User user = new User(1, "19900000001");
        String jwt = "header.payload.signature";

        Result result = new Result(Code.LOGIN_OK, user, jwt);

        assertThat(result.getCode()).isEqualTo(Code.LOGIN_OK);
        assertThat(result.getMsg()).isEqualTo(jwt);
        assertThat(result.getData()).isSameAs(user);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("success() 系列方法的业务码固定为 0")
    void successFactories() {
        assertThat(Result.success().getCode()).isEqualTo(Code.SUCCESS);
        assertThat(Result.success("data").getData()).isEqualTo("data");
        assertThat(Result.success("data", "自定义消息").getMsg()).isEqualTo("自定义消息");
        assertThat(Result.success(Code.SELECT_OK, "data", "查询成功").getCode())
                .isEqualTo(Code.SELECT_OK);
    }

    @Test
    @DisplayName("failure() 系列方法被判为失败，且 data 为 null")
    void failureFactories() {
        Result plain = Result.failure("参数错误");
        assertThat(plain.getCode()).isEqualTo(Code.FAILURE);
        assertThat(plain.isFailure()).isTrue();
        assertThat(plain.getData()).isNull();

        Result coded = Result.failure(Code.PASSWORD_ERR, "密码错误");
        assertThat(coded.getCode()).isEqualTo(Code.PASSWORD_ERR);
        assertThat(coded.isFailure()).isTrue();
    }

    @Test
    @DisplayName("两参构造器不设置 msg，getMsg 返回 null 而不是空串")
    void twoArgConstructorLeavesMsgNull() {
        Result result = new Result(Code.SELECT_OK, "data");
        assertThat(result.getMsg()).isNull();
    }

    @Test
    @DisplayName("setCode 改成失败码后 isSuccess 立即翻转")
    void statusFollowsCode() {
        Result result = Result.success("data");
        assertThat(result.isSuccess()).isTrue();

        result.setCode(Code.ADD_ERR);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
    }

    /** 只为测试准备的最小载荷，不依赖真实 User 实体字段。 */
    private static class User {
        private final Integer id;
        private final String phone;

        User(Integer id, String phone) {
            this.id = id;
            this.phone = phone;
        }
    }
}
