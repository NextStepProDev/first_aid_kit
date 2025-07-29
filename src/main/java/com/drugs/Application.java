package com.drugs;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Application has started successfully.");
    }
//	http://localhost:8080/api/drugs
//	http://localhost:8080/swagger-ui.html

    @PreDestroy
    @SuppressWarnings("unused")
    public void onExit() {
        log.info("Application is shutting down.");
    }
}
