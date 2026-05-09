package com.hamtech.bookstorepromotionservice.client;

import java.util.List;
import java.util.UUID;

public class ValidateBookIdsResponse {
    private boolean allValid;
    private List<UUID> invalidBookIds;

    public boolean isAllValid() {
        return allValid;
    }

    public void setAllValid(boolean allValid) {
        this.allValid = allValid;
    }

    public List<UUID> getInvalidBookIds() {
        return invalidBookIds;
    }

    public void setInvalidBookIds(List<UUID> invalidBookIds) {
        this.invalidBookIds = invalidBookIds;
    }
}

