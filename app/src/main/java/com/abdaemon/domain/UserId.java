package com.abdaemon.domain;

public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("UserId cannot be blank");
    }
}
