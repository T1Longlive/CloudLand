package com.cloudland.controller.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Result} 成功/失败语义的单元测试（纯逻辑，无 Spring、无数据库）。
 *
 * <p>核心契约只有一条：<b>{@code isSuccess()} / {@code isFailure()} 必须与 {@link Code}
 * 的分类一致</b>。这条看着简单，但它是所有前端分支判断的依据 ——
 * 一旦某个工厂方法塞错了码，前端会把失败当成功（或反之），而编译期毫无提示。
 */
class ResultTest {

    @Test
    @DisplayName("success()：码是 SUCCESS、判为成功、不是失败")
    void successWithoutData() {
        Result r = Result.success();

        assertThat(r.getCode()).isEqualTo(Code.SUCCESS);
        assertThat(r.getData()).isNull();
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.isFailure()).isFalse();
    }

    @Test
    @DisplayName("success(data)：数据原样带出，仍然是成功")
    void successWithData() {
        Result r = Result.success(Collections.singletonList("x"));

        assertThat(r.getCode()).isEqualTo(Code.SUCCESS);
        assertThat(r.getData()).isEqualTo(Collections.singletonList("x"));
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("failure(msg)：码是 FAILURE、判为失败、数据为空")
    void failureWithMessage() {
        Result r = Result.failure("出错了");

        assertThat(r.getCode()).isEqualTo(Code.FAILURE);
        assertThat(r.getMsg()).isEqualTo("出错了");
        assertThat(r.getData()).isNull();
        assertThat(r.isFailure()).isTrue();
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("带业务码的 success 要判成功、带业务码的 failure 要判失败")
    void customCodesFollowCodeClassification() {
        // 这两个码是典型的「看起来都是 2xxxx，但一个成功一个失败」——
        // 正是白名单分类容易出错的地方
        assertThat(Result.success(Code.LOGIN_OK, null, "登录成功").isSuccess()).isTrue();
        assertThat(Result.failure(Code.POWER_ERR, "权限不足").isFailure()).isTrue();

        assertThat(Result.success(Code.LOGIN_OK, null, "登录成功").isFailure()).isFalse();
        assertThat(Result.failure(Code.POWER_ERR, "权限不足").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("两参构造器不带消息，msg 为 null（不能默认成成功/失败文案）")
    void twoArgConstructorLeavesMessageNull() {
        Result r = new Result(Code.SELECT_OK, "payload");

        assertThat(r.getCode()).isEqualTo(Code.SELECT_OK);
        assertThat(r.getData()).isEqualTo("payload");
        assertThat(r.getMsg()).isNull();
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("码可以后置修改，语义随之翻转（setCode 之后判断要跟着变）")
    void codeIsMutableAndSemanticsFollow() {
        Result r = Result.success();

        r.setCode(Code.FAILURE);

        assertThat(r.isSuccess()).isFalse();
        assertThat(r.isFailure()).isTrue();
    }
}
