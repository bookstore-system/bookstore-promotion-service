package com.hamtech.bookstorepromotionservice.client;

import java.util.UUID;

public class BookExistsResponse {
    private boolean exists;
    private UUID bookId;
    private String title;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public UUID getBookId() {
        return bookId;
    }

    public void setBookId(UUID bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

