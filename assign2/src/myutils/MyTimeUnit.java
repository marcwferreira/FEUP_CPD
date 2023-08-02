package myutils;

public enum MyTimeUnit {
    NANOSECONDS(1),
    MICROSECONDS(1000),
    MILLISECONDS(1000000),
    SECONDS(1000000000),
    MINUTES(60000000000L),
    HOURS(3600000000000L),
    DAYS(86400000000000L);

    private final long nanos;

    MyTimeUnit(long nanos) {
        this.nanos = nanos;
    }

    public long toNanos(long value) {
        return value * nanos;
    }

    public long toMicros(long value) {
        return value * (nanos / 1000);
    }

    public long toMillis(long value) {
        return value * (nanos / 1000000);
    }

    public long toSeconds(long value) {
        return value * (nanos / 1000000000);
    }

    public long toMinutes(long value) {
        return value * (nanos / 60000000000L);
    }

    public long toHours(long value) {
        return value * (nanos / 3600000000000L);
    }

    public long toDays(long value) {
        return value * (nanos / 86400000000000L);
    }
}
