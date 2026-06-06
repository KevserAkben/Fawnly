package com.fawnly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FawnlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FawnlyApplication.class, args);
    }
}
