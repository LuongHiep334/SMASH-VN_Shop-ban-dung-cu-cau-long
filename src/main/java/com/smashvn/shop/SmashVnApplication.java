package com.smashvn.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class SmashVnApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmashVnApplication.class, args);
    }
}
