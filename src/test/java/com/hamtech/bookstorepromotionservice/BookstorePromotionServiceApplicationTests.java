package com.hamtech.bookstorepromotionservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.hamtech.bookstorepromotionservice.client.BookServiceClient;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
import com.hamtech.bookstorepromotionservice.repository.PromotionReservationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest(classes = BookstorePromotionServiceApplication.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class BookstorePromotionServiceApplicationTests {

    @MockBean
    PromotionRepository promotionRepository;

    @MockBean
    BookServiceClient bookServiceClient;

    @MockBean
    PromotionReservationRepository promotionReservationRepository;

    @MockBean
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

}
