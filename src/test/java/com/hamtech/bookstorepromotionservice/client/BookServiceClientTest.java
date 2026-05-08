package com.hamtech.bookstorepromotionservice.client;

import com.hamtech.bookstorepromotionservice.BookstorePromotionServiceApplication;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BookstorePromotionServiceApplication.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "jwt.signerKey=unit-test-signer-key"
})
class BookServiceClientTest {

    @MockBean
    PromotionRepository promotionRepository;

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
    void validateBookIds_postsRequest_andReadsResponse() throws Exception {
        UUID id1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID id2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "allValid": true,
                          "invalidBookIds": []
                        }
                        """));

        ValidateBookIdsRequest req = new ValidateBookIdsRequest();
        req.setBookIds(List.of(id1, id2));

        ValidateBookIdsResponse res = bookServiceClient.validateBookIds(req);

        assertThat(res.isAllValid()).isTrue();
        assertThat(res.getInvalidBookIds()).isEmpty();

        RecordedRequest recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/api/v1/books/validate-ids");

        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"bookIds\"");
        assertThat(body).contains(id1.toString());
        assertThat(body).contains(id2.toString());
    }
}

