package com.anujbohre.uqlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point. Running main() starts Spring, which scans this package and below for components. */
@SpringBootApplication
public class UqLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(UqLabApplication.class, args);
    }
}
