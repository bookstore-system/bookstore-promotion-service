package com.hamtech.bookstorepromotionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "book-service", url = "${clients.book-service.url:}")
public interface BookServiceClient {

    @PostMapping("/api/v1/books/validate-ids")
    ValidateBookIdsResponse validateBookIds(@RequestBody ValidateBookIdsRequest request);
}

