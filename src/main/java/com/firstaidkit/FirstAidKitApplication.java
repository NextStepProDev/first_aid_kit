package com.firstaidkit;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class FirstAidKitApplication implements CommandLineRunner {

    static void main(String[] args) {
        SpringApplication.run(FirstAidKitApplication.class, args);
    }

    @Override
    public void run(String @NonNull ... args) {
        log.info("FirstAidKitApplication has started successfully.");
    }

    @PreDestroy
    public void onExit() {
        log.info("FirstAidKitApplication is shutting down.");
    }
}
