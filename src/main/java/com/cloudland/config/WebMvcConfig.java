package com.cloudland.config;

import com.cloudland.Interceptor.MyInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private MyInterceptor myInterceptor;

    @Value("${access-file.resource-handler1}")
    private String resourceHandler1;
    @Value("${access-file.resource-handler2}")
    private String resourceHandler2;
    @Value("${access-file.resource-handler3}")
    private String resourceHandler3;
    @Value("${access-file.location}")
    private String location;
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourceHandler1).addResourceLocations("file:///" + location + "/UserIcon/");
        registry.addResourceHandler(resourceHandler2).addResourceLocations("file:///" + location + "/LandFile/");
        registry.addResourceHandler(resourceHandler3).addResourceLocations("file:///" + location + "/Product/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/resource/**", "/land/page", "/land/{id}", "/product/page", "/product/{id}", "/msg", "/msg/mail", "/alipay/notify", "/error");
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> originList = new ArrayList<>();
        for (String origin : allowedOrigins.split(",")) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.isEmpty()) {
                originList.add(trimmedOrigin);
            }
        }
        config.setAllowedOrigins(originList.isEmpty() ? Collections.singletonList("http://localhost:8080") : originList);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("token", "Content-Type", "remember", "frond", "code", "contact", "forgetPassword"));
        config.setExposedHeaders(Collections.singletonList("updatedToken"));
        config.setMaxAge(-1L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
