package com.abdaemon.domain;

public record RequestId(String value) {
    public RequestId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("RequestId cannot be blank");
    }
}
