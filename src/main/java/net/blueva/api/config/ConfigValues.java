package net.blueva.api.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

final class ConfigValues {
    private ConfigValues() {
    }

    static Object parse(String raw) {
        return parse(raw, null);
    }

    static Object parse(String raw, ConfigFormat format) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (isTripleQuoted(value)) {
            return unquoteTriple(value);
        }
        if (isQuoted(value)) {
            return unquote(value);
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseList(value.substring(1, value.length() - 1), format);
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseMap(value.substring(1, value.length() - 1), format);
        }
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        ConfigDateTime dateTime = parseDateTime(value);
        if (dateTime != null) {
            return dateTime;
        }
        Object special = parseSpecialNumber(value);
        if (special != null) {
            return special;
        }
        try {
            String numberValue = value.replace("_", "");
            if (numberValue.matches("[+-]?0[xX][0-9a-fA-F]+")) {
                long number = Long.parseLong(stripSign(numberValue).substring(2), 16) * sign(numberValue);
                return narrow(number);
            }
            if (numberValue.matches("[+-]?0[oO][0-7]+")) {
                long number = Long.parseLong(stripSign(numberValue).substring(2), 8) * sign(numberValue);
                return narrow(number);
            }
            if (numberValue.matches("[+-]?0[bB][01]+")) {
                long number = Long.parseLong(stripSign(numberValue).substring(2), 2) * sign(numberValue);
                return narrow(number);
            }
            if (!numberValue.contains(".") && !numberValue.contains("e") && !numberValue.contains("E")) {
                long number = Long.parseLong(numberValue);
                return narrow(number);
            }
            return Double.parseDouble(numberValue);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    static String yaml(Object value) {
        if (value instanceof List) {
            return list((List<?>) value, false);
        }
        if (value instanceof Map) {
            return map((Map<?, ?>) value, false);
        }
        return scalar(value, false);
    }

    static String toml(Object value) {
        if (value instanceof List) {
            return list((List<?>) value, true);
        }
        if (value instanceof Map) {
            return map((Map<?, ?>) value, true);
        }
        return scalar(value, true);
    }

    static String canonical(Object value) {
        if (value instanceof List) {
            StringBuilder builder = new StringBuilder("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(canonical(list.get(i)));
            }
            return builder.append(']').toString();
        }
        if (value instanceof Map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(String.valueOf(entry.getKey())).append(':').append(canonical(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (value instanceof ConfigDateTime) {
            return ConfigDateTime.class.getName() + ":" + ((ConfigDateTime) value).raw();
        }
        return value == null ? "null" : value.getClass().getName() + ":" + String.valueOf(value);
    }

    static int inlineCommentIndex(String line) {
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
            } else if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
            } else if (c == '#' && !single && !doubleQuote && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) {
                return i;
            }
        }
        return -1;
    }

    static int separatorIndex(String line, char separator) {
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        int bracket = 0;
        int brace = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
            } else if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
            } else if (!single && !doubleQuote) {
                if (c == '[') {
                    bracket++;
                } else if (c == ']') {
                    bracket--;
                } else if (c == '{') {
                    brace++;
                } else if (c == '}') {
                    brace--;
                } else if (c == separator && bracket == 0 && brace == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static List<String> logicalLines(String text) {
        return logicalLines(text, null);
    }

    static List<String> logicalLines(String text, ConfigFormat format) {
        List<String> result = new ArrayList<>();
        String[] lines = text == null ? new String[0] : text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder current = new StringBuilder();
        int balance = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(line);
            balance += bracketBalance(line);
            if (balance < 0 && format != null) {
                throw new ConfigParseException(format, i + 1, 1, "Unexpected closing bracket");
            }
            boolean quoteOpen = hasUnclosedQuote(current.toString());
            if (balance <= 0 && !quoteOpen) {
                result.add(current.toString());
                current.setLength(0);
                balance = 0;
            }
        }
        if (balance > 0 && format != null) {
            throw new ConfigParseException(format, lines.length, 1, "Unclosed inline list or table");
        }
        if (current.length() > 0) {
            if (format != null && hasUnclosedQuote(current.toString())) {
                throw new ConfigParseException(format, lines.length, 1, "Unclosed quoted string");
            }
            result.add(current.toString());
        }
        return result;
    }

    static boolean hasUnclosedQuote(String line) {
        if (containsTripleQuote(line)) {
            return false;
        }
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
            } else if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
            }
        }
        return single || doubleQuote;
    }

    private static List<Object> parseList(String raw, ConfigFormat format) {
        List<Object> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        int nested = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
                current.append(c);
                continue;
            }
            if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
                current.append(c);
                continue;
            }
            if (!single && !doubleQuote) {
                if (c == '[') {
                    nested++;
                } else if (c == '{') {
                    nested++;
                } else if (c == ']') {
                    nested--;
                } else if (c == '}') {
                    nested--;
                } else if (c == ',' && nested == 0) {
                    result.add(parse(current.toString(), format));
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 || raw.trim().length() > 0) {
            result.add(parse(current.toString(), format));
        }
        return result;
    }

    private static Map<String, Object> parseMap(String raw, ConfigFormat format) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (String entry : splitTopLevel(raw)) {
            if (entry.trim().isEmpty()) {
                continue;
            }
            int equals = separatorIndex(entry, '=');
            int colon = separatorIndex(entry, ':');
            int separator;
            if (equals < 0) {
                separator = colon;
            } else if (colon < 0) {
                separator = equals;
            } else {
                separator = Math.min(equals, colon);
            }
            if (separator < 0) {
                result.put(entry.trim(), "");
                continue;
            }
            String key = entry.substring(0, separator).trim();
            if (isQuoted(key)) {
                key = unquote(key);
            }
            if (result.containsKey(key)) {
                if (format != null) {
                    throw new ConfigParseException(format, 1, 1, "Duplicate inline map key '" + key + "'");
                }
                throw new IllegalArgumentException("Duplicate inline map key '" + key + "'");
            }
            result.put(key, parse(entry.substring(separator + 1), format));
        }
        return result;
    }

    private static List<String> splitTopLevel(String raw) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        int nested = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
                current.append(c);
                continue;
            }
            if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
                current.append(c);
                continue;
            }
            if (!single && !doubleQuote) {
                if (c == '[' || c == '{') {
                    nested++;
                } else if (c == ']' || c == '}') {
                    nested--;
                } else if (c == ',' && nested == 0) {
                    result.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 || raw.trim().length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static String list(List<?> list, boolean toml) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Object value = list.get(i);
            if (value instanceof List) {
                builder.append(list((List<?>) value, toml));
            } else if (value instanceof Map) {
                builder.append(map((Map<?, ?>) value, toml));
            } else {
                builder.append(scalar(value, toml));
            }
        }
        return builder.append(']').toString();
    }

    private static String map(Map<?, ?> map, boolean toml) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            String key = String.valueOf(entry.getKey());
            builder.append(toml ? ConfigPath.tomlKey(key) : ConfigPath.yamlKey(key));
            builder.append(toml ? " = " : ": ");
            Object value = entry.getValue();
            if (value instanceof List) {
                builder.append(list((List<?>) value, toml));
            } else if (value instanceof Map) {
                builder.append(map((Map<?, ?>) value, toml));
            } else {
                builder.append(scalar(value, toml));
            }
        }
        return builder.append('}').toString();
    }

    private static String scalar(Object value, boolean toml) {
        if (value == null) {
            return toml ? "\"\"" : "";
        }
        if (toml && value instanceof ConfigDateTime) {
            return ((ConfigDateTime) value).raw();
        }
        if (toml && value instanceof Double) {
            double number = (Double) value;
            if (Double.isNaN(number)) {
                return "nan";
            }
            if (Double.isInfinite(number)) {
                return number < 0 ? "-inf" : "inf";
            }
        }
        if (toml && value instanceof Float) {
            float number = (Float) value;
            if (Float.isNaN(number)) {
                return "nan";
            }
            if (Float.isInfinite(number)) {
                return number < 0 ? "-inf" : "inf";
            }
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        String string = String.valueOf(value);
        if (toml || needsYamlQuotes(string)) {
            return "\"" + escape(string) + "\"";
        }
        return string;
    }

    private static boolean needsYamlQuotes(String value) {
        if (value.isEmpty()) {
            return true;
        }
        String lower = value.toLowerCase();
        if ("true".equals(lower) || "false".equals(lower) || "null".equals(lower) || "~".equals(value)) {
            return true;
        }
        if (value.startsWith(" ") || value.endsWith(" ") || value.startsWith("[") || value.startsWith("{")
                || value.startsWith("#") || value.contains(": ") || value.contains(" #") || value.contains("\n")
                || value.indexOf('\t') >= 0 || value.indexOf('"') >= 0) {
            return true;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static boolean isQuoted(String value) {
        return value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")));
    }

    static boolean isTripleQuoted(String value) {
        return value.length() >= 6
                && ((value.startsWith("\"\"\"") && value.endsWith("\"\"\""))
                || (value.startsWith("'''") && value.endsWith("'''")));
    }

    static boolean containsTripleQuote(String value) {
        return value != null && (value.indexOf("\"\"\"") >= 0 || value.indexOf("'''") >= 0);
    }

    static String unquoteTriple(String value) {
        String inner = value.substring(3, value.length() - 3);
        if (value.startsWith("'''")) {
            return inner;
        }
        return unquote("\"" + inner + "\"");
    }

    static String unquote(String value) {
        String inner = value.substring(1, value.length() - 1);
        if (value.startsWith("'")) {
            return inner;
        }
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaped) {
                if (c == 'n') {
                    builder.append('\n');
                } else if (c == 't') {
                    builder.append('\t');
                } else if (c == 'r') {
                    builder.append('\r');
                } else if (c == 'b') {
                    builder.append('\b');
                } else if (c == 'f') {
                    builder.append('\f');
                } else if (c == '"' || c == '\\' || c == '/') {
                    builder.append(c);
                } else if (c == 'u' && i + 4 < inner.length()) {
                    builder.append((char) Integer.parseInt(inner.substring(i + 1, i + 5), 16));
                    i += 4;
                } else if (c == 'U' && i + 8 < inner.length()) {
                    int codePoint = (int) Long.parseLong(inner.substring(i + 1, i + 9), 16);
                    builder.append(Character.toChars(codePoint));
                    i += 8;
                } else {
                    builder.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                builder.append(c);
            }
        }
        if (escaped) {
            builder.append('\\');
        }
        return builder.toString();
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static ConfigDateTime parseDateTime(String value) {
        try {
            if (value.matches("\\d{4}-\\d{2}-\\d{2}[Tt ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([Zz]|[+-]\\d{2}:\\d{2})")) {
                return new ConfigDateTime(ConfigDateTime.Type.OFFSET_DATE_TIME, value, OffsetDateTime.parse(value.replace(' ', 'T').replace('z', 'Z')));
            }
            if (value.matches("\\d{4}-\\d{2}-\\d{2}[Tt ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")) {
                return new ConfigDateTime(ConfigDateTime.Type.LOCAL_DATE_TIME, value, LocalDateTime.parse(value.replace(' ', 'T')));
            }
            if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return new ConfigDateTime(ConfigDateTime.Type.LOCAL_DATE, value, LocalDate.parse(value));
            }
            if (value.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")) {
                return new ConfigDateTime(ConfigDateTime.Type.LOCAL_TIME, value, LocalTime.parse(value));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static Object parseSpecialNumber(String value) {
        if ("inf".equals(value) || "+inf".equals(value)) {
            return Double.POSITIVE_INFINITY;
        }
        if ("-inf".equals(value)) {
            return Double.NEGATIVE_INFINITY;
        }
        if ("nan".equals(value) || "+nan".equals(value) || "-nan".equals(value)) {
            return Double.NaN;
        }
        return null;
    }

    private static Object narrow(long number) {
        if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
            return (int) number;
        }
        return number;
    }

    private static String stripSign(String value) {
        return value.startsWith("+") || value.startsWith("-") ? value.substring(1) : value;
    }

    private static int sign(String value) {
        return value.startsWith("-") ? -1 : 1;
    }

    private static int bracketBalance(String line) {
        boolean single = false;
        boolean doubleQuote = false;
        boolean escaped = false;
        int balance = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && doubleQuote) {
                escaped = true;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                single = !single;
            } else if (c == '"' && !single) {
                doubleQuote = !doubleQuote;
            } else if (!single && !doubleQuote) {
                if (c == '[' || c == '{') {
                    balance++;
                } else if (c == ']' || c == '}') {
                    balance--;
                }
            }
        }
        return balance;
    }
}
