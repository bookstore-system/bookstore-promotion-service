package com.hamtech.bookstorepromotionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "book-service", url = "${clients.book-service.url:}")
public interface BookServiceClient {

    @GetMapping("/api/v1/books/{bookId}/exists")
    BookServiceApiResponse<BookExistsResponse> checkBookExists(@PathVariable("bookId") java.util.UUID bookId);

    @PostMapping("/api/v1/books/validate-ids")
    ValidateBookIdsResponse validateBookIds(@RequestBody ValidateBookIdsRequest request);
}

