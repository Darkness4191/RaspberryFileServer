package de.dragon.RFS;

import java.time.Instant;

public class TimeStamp {

    public static final long USER_CACHE_TIMEOUT = 30;
    public static final long INSTANCE_TIMEOUT = 5 * 60;
    public static final long SESSION_TIMEOUT = 60 * 60;
    public static final long SESSION_MAX_AGE = 3 * 60 * 60;

    private long timestamp;

    public TimeStamp() {
        this.timestamp = Instant.now().getEpochSecond();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
