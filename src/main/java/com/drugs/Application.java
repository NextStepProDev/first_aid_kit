package com.drugs;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        logger.info("Application has started successfully.");
    }
//	http://localhost:8080/api/drugs
//	http://localhost:8080/swagger-ui.html

    @PreDestroy
    public void onExit() {
        // Logowanie przy zakończeniu działania aplikacji
        logger.info("Application is shutting down.");
    }
}
