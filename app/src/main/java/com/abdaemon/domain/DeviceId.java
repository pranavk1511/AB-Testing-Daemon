package com.abdaemon.domain;

public record DeviceId(String value) {
    public DeviceId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("DeviceId cannot be blank");
    }
}
