package com.cloudland.controller.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link Code} 状态码分类的**不变量测试**（纯逻辑，无 Spring、无数据库）。
 *
 * <p>为什么用反射遍历而不是一条条写死：{@code Code} 里全是常量，
 * {@code isSuccess} 是一份**手写白名单**。这个结构有个典型的失效方式 ——
 * <b>有人加了一个新的 {@code XXX_OK}，却忘了同步加进白名单</b>，
 * 于是那个"成功"会被全线当成失败（或反之），而且编译通过、单元测试也未必覆盖到。
 *
 * <p>把它写成「所有 {@code *_OK} 必须被判为成功」这样的不变量，
 * **以后每加一个常量都会自动被检查一遍** —— 这是写死断言做不到的。
 *
 * <p>另外每个不变量都先断言"确实扫到了东西"：
 * 否则反射筛选写错了（比如把后缀写成 {@code _OKK}），测试会**空跑通过**，
 * 比不写还危险。
 */
class CodeTest {

    @Test
    @DisplayName("不变量：每一个 *_OK 常量都必须被判为成功")
    void everyOkConstantIsSuccess() throws IllegalAccessException {
        List<Field> okFields = integerConstantsEndingWith("_OK");

        // 先确认真的扫到了东西 —— 否则下面的循环会空跑通过，等于没测
        assertThat(okFields).as("没扫到任何 *_OK 常量，说明反射筛选条件写错了").isNotEmpty();

        List<String> misclassified = new ArrayList<String>();
        for (Field f : okFields) {
            Integer value = (Integer) f.get(null);
            if (!Code.isSuccess(value)) {
                misclassified.add(f.getName() + " = " + value);
            }
        }
        assertThat(misclassified)
                .as("以下 *_OK 常量没被 Code.isSuccess 归类为成功（加了常量忘了同步白名单？）")
                .isEmpty();
    }

    @Test
    @DisplayName("不变量：每一个 *_ERR 常量都必须被判为失败")
    void everyErrConstantIsFailure() throws IllegalAccessException {
        List<Field> errFields = integerConstantsEndingWith("_ERR");

        assertThat(errFields).as("没扫到任何 *_ERR 常量，说明反射筛选条件写错了").isNotEmpty();

        List<String> misclassified = new ArrayList<String>();
        for (Field f : errFields) {
            Integer value = (Integer) f.get(null);
            if (!Code.isFailure(value)) {
                misclassified.add(f.getName() + " = " + value);
            }
        }
        assertThat(misclassified)
                .as("以下 *_ERR 常量竟然被判为成功")
                .isEmpty();
    }

    @Test
    @DisplayName("通用标识：SUCCESS 判成功、FAILURE 判失败")
    void genericMarkers() {
        assertThat(Code.isSuccess(Code.SUCCESS)).isTrue();
        assertThat(Code.isFailure(Code.FAILURE)).isTrue();
        assertThat(Code.isSuccess(Code.FAILURE)).isFalse();
    }

    @Test
    @DisplayName("null 状态码判为失败，且不能抛异常（防御性：未登录/未设置时不该崩）")
    void nullIsFailureWithoutThrowing() {
        assertThatCode(() -> Code.isFailure(null)).doesNotThrowAnyException();
        assertThat(Code.isSuccess(null)).isFalse();
        assertThat(Code.isFailure(null)).isTrue();
    }

    @Test
    @DisplayName("未定义的码一律判为失败（isSuccess 是白名单，不是范围判断）")
    void unknownCodeIsFailure() {
        assertThat(Code.isSuccess(99999)).isFalse();
        assertThat(Code.isSuccess(-99999)).isFalse();
        // 一个"看起来像成功"但没登记过的码，依然是失败 —— 这是白名单语义的关键
        assertThat(Code.isSuccess(10009)).isFalse();
    }

    /** 取出 Code 里所有 static final Integer 字段中名字以给定后缀结尾的 */
    private static List<Field> integerConstantsEndingWith(String suffix) {
        List<Field> hit = new ArrayList<Field>();
        for (Field f : Code.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType() != Integer.class) {
                continue;
            }
            if (f.getName().endsWith(suffix)) {
                hit.add(f);
            }
        }
        return hit;
    }
}
