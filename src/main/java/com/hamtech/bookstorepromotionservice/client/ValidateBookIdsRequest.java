package com.hamtech.bookstorepromotionservice.client;

import java.util.List;
import java.util.UUID;

public class ValidateBookIdsRequest {
    private List<UUID> bookIds;

    public List<UUID> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<UUID> bookIds) {
        this.bookIds = bookIds;
    }
}

