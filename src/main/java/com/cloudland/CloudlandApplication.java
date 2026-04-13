package com.cloudland;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.cloudland.mapper")
public class CloudlandApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudlandApplication.class, args);
    }
}
