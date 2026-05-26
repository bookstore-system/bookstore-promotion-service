package com.hamtech.bookstorepromotionservice.client;

import com.hamtech.bookstorepromotionservice.BookstorePromotionServiceApplication;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
import com.hamtech.bookstorepromotionservice.repository.PromotionReservationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BookstorePromotionServiceApplication.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "jwt.signerKey=unit-test-signer-key",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class BookServiceClientTest {

    @MockBean
    PromotionRepository promotionRepository;

    @MockBean
    PromotionReservationRepository promotionReservationRepository;

    @MockBean
    RabbitTemplate rabbitTemplate;

    static MockWebServer mockWebServer;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (mockWebServer == null) {
            mockWebServer = new MockWebServer();
            try {
                mockWebServer.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        registry.add("clients.book-service.url", () -> mockWebServer.url("/").toString());
    }

    @BeforeAll
    static void beforeAll() {
        // started in @DynamicPropertySource (runs before @BeforeAll)
    }

    @AfterAll
    static void afterAll() {
        if (mockWebServer != null) {
            try {
                mockWebServer.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    @Autowired
    BookServiceClient bookServiceClient;

    @Test
    void checkBookExists_getsRequest_andReadsResponse() throws Exception {
        UUID bookId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "code": 200,
                          "message": "Success",
                          "data": {
                            "exists": true,
                            "bookId": "123e4567-e89b-12d3-a456-426614174000",
                            "title": "Clean Code"
                          }
                        }
                        """));

        BookServiceApiResponse<BookExistsResponse> wrapper = bookServiceClient.checkBookExists(bookId);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getCode()).isEqualTo(200);
        assertThat(wrapper.getData()).isNotNull();
        assertThat(wrapper.getData().isExists()).isTrue();
        assertThat(wrapper.getData().getBookId()).isEqualTo(bookId);
        assertThat(wrapper.getData().getTitle()).isEqualTo("Clean Code");

        RecordedRequest recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/api/v1/books/" + bookId + "/exists");
    }
}

