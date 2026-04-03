package com.interview.aichat;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//
@SpringBootApplication
public class AiChat {
    public static void main(String[] args) {
        // 强制启用bootstrap配置加载
        System.setProperty("spring.cloud.bootstrap.enabled", "true");
        SpringApplication.run(AiChat.class, args);
    }
}