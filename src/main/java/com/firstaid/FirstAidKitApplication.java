package com.firstaid;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@Slf4j
@SpringBootApplication
@EnableCaching
public class FirstAidKitApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(FirstAidKitApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("FirstAidKitApplication has started successfully.");
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void onExit() {
        log.info("FirstAidKitApplication is shutting down.");
    }
}
