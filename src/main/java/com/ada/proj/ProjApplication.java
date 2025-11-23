package com.ada.proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication
@EnableCaching
public class ProjApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjApplication.class, args);
    }

}
