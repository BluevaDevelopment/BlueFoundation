package net.blueva.api.config;

/** Parsed TOML date/time value. */
public final class ConfigDateTime {
    public enum Type {
        OFFSET_DATE_TIME,
        LOCAL_DATE_TIME,
        LOCAL_DATE,
        LOCAL_TIME
    }

    private final Type type;
    private final String raw;
    private final Object value;

    ConfigDateTime(Type type, String raw, Object value) {
        this.type = type;
        this.raw = raw;
        this.value = value;
    }

    public Type type() {
        return type;
    }

    public String raw() {
        return raw;
    }

    public Object value() {
        return value;
    }

    @Override
    public String toString() {
        return raw;
    }
}
