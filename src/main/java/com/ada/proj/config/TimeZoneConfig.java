package com.ada.proj.config;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TimeZoneConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void configureTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(KST));
        log.info("Default timezone set to {}", KST);
    }
}
