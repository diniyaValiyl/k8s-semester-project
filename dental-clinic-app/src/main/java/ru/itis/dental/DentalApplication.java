package ru.itis.dental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DentalApplication {
    public static void main(String[] args) {
        SpringApplication.run(DentalApplication.class, args);
        //docker-compose down
        //docker-compose up --build
    }
}