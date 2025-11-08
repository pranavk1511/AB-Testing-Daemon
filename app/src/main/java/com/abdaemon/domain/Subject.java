package com.abdaemon.domain;

import java.util.Optional;

/** Entity we assign to (user/device/session). */
public final class Subject {
    private final UserId userId;
    private final DeviceId deviceId;
    private final RequestId requestId;

    private Subject(UserId userId, DeviceId deviceId, RequestId requestId) {
        this.userId = userId; this.deviceId = deviceId; this.requestId = requestId;
    }

    public static Subject of(UserId userId, DeviceId deviceId, RequestId requestId) {
        if (userId == null && deviceId == null && requestId == null)
            throw new IllegalArgumentException("At least one identifier required");
        return new Subject(userId, deviceId, requestId);
    }

    public Optional<UserId> userId()     { return Optional.ofNullable(userId); }
    public Optional<DeviceId> deviceId() { return Optional.ofNullable(deviceId); }
    public Optional<RequestId> requestId(){ return Optional.ofNullable(requestId); }
}
