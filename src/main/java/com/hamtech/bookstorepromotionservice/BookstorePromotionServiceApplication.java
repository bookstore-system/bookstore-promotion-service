package com.hamtech.bookstorepromotionservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableRabbit
@EnableFeignClients
public class BookstorePromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstorePromotionServiceApplication.class, args);
    }

}
