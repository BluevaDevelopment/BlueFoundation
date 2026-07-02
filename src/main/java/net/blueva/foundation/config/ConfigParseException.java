package net.blueva.foundation.config;

/** Thrown when BlueFoundation cannot parse a configuration document safely. */
public class ConfigParseException extends IllegalArgumentException {
    private final ConfigFormat format;
    private final int line;
    private final int column;
    private final String source;

    public ConfigParseException(ConfigFormat format, int line, int column, String message) {
        this(format, line, column, message, null);
    }

    public ConfigParseException(ConfigFormat format, int line, int column, String message, String source) {
        super(format + " config parse error"
                + (source == null || source.isBlank() ? "" : " in " + source)
                + " at line " + line + ", column " + column + ": " + message);
        this.format = format;
        this.line = line;
        this.column = column;
        this.source = source;
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

    public String source() {
        return source;
    }
}
