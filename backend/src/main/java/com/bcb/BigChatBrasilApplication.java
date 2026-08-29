package com.bcb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BigChatBrasilApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigChatBrasilApplication.class, args);
    }
}
