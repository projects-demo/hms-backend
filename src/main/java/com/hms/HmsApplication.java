package com.hms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { FlywayAutoConfiguration.class })
@EnableCaching
@EnableJpaAuditing
@EntityScan(basePackages = "com.hms")
@EnableScheduling

public class HmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HmsApplication.class, args);
    }
}
