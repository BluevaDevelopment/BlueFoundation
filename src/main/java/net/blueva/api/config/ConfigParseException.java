package net.blueva.api.config;

/** Thrown when BlueAPI cannot parse a configuration document safely. */
public class ConfigParseException extends IllegalArgumentException {
    private final ConfigFormat format;
    private final int line;
    private final int column;

    public ConfigParseException(ConfigFormat format, int line, int column, String message) {
        super(format + " config parse error at line " + line + ", column " + column + ": " + message);
        this.format = format;
        this.line = line;
        this.column = column;
    }

    public ConfigFormat format() {
        return format;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
