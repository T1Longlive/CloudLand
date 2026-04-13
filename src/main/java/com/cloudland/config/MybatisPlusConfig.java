package com.cloudland.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 定义MybatisPlus的配置类
@Configuration
public class MybatisPlusConfig {
    // 定义一个bean
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建一个过滤器容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 向容器中添加一个分页的容器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 返回这个容器
        return interceptor;
    }
}

