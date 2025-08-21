package com.example.calmall;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot アプリケーションのエントリーポイント
 */
@SpringBootApplication
@EnableScheduling
public class CalmallApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalmallApplication.class, args);
    }
}
