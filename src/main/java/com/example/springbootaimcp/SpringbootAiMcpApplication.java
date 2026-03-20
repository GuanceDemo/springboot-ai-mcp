package com.example.springbootaimcp;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringbootAiMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootAiMcpApplication.class, args);
    }
}
