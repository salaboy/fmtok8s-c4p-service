package com.salaboy.conferences.c4p.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class C4PApplication {

    public static void main(String[] args) {
        SpringApplication.run(C4PApplication.class, args);
    }


}
