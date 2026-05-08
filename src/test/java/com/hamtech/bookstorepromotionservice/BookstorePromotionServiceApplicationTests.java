package com.hamtech.bookstorepromotionservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;

@SpringBootTest(classes = BookstorePromotionServiceApplication.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class BookstorePromotionServiceApplicationTests {

    @MockBean
    PromotionRepository promotionRepository;

    @Test
    void contextLoads() {
    }

}
