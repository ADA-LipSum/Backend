package com.ada.proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.ada.proj.config.CookieProperties;
import com.ada.proj.config.CorsProperties;
import com.ada.proj.config.JwtProperties;
import com.ada.proj.config.AutoIncrementProperties;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({CookieProperties.class, CorsProperties.class, JwtProperties.class, AutoIncrementProperties.class,
    })
public class ProjApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjApplication.class, args);
    }

}
