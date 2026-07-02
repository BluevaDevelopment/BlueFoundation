package net.blueva.foundation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON codec backed by Gson. Converts between JSON text and BlueFoundation's
 * {@link ConfigDocument} tree, preserving maps as sections and arrays as lists.
 */
final class JsonConfigCodec implements ConfigCodec {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    @Override
    public ConfigDocument read(String text) {
        ConfigDocument document = new ConfigDocument();
        if (text == null || text.trim().isEmpty()) {
            return document;
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (Exception e) {
            throw new ConfigParseException(ConfigFormat.JSON, 1, 1, "Invalid JSON: " + e.getMessage());
        }
        if (root.isJsonNull()) {
            return document;
        }
        if (!root.isJsonObject()) {
            throw new ConfigParseException(ConfigFormat.JSON, 1, 1, "JSON root must be an object");
        }
        fillNode(document.root(), root.getAsJsonObject());
        return document;
    }

    @Override
    public String write(ConfigDocument document) {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, ConfigNode> entry : document.root().children().entrySet()) {
            JsonElement child = toJson(entry.getValue());
            if (child != null) {
                root.add(entry.getKey(), child);
            }
        }
        return GSON.toJson(root);
    }

    @Override
    public String extension() {
        return "json";
    }

    private static void fillNode(ConfigNode node, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            ConfigNode child = new ConfigNode();
            setValue(child, entry.getValue());
            node.putChild(entry.getKey(), child);
        }
    }

    private static void setValue(ConfigNode node, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            node.setValue(null);
        } else if (element.isJsonObject()) {
            fillNode(node, element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            node.setValue(toList(element.getAsJsonArray()));
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                node.setValue(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                node.setValue(number);
            } else {
                node.setValue(primitive.getAsString());
            }
        }
    }

    private static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) {
                list.add(null);
            } else if (element.isJsonObject()) {
                list.add(toMap(element.getAsJsonObject()));
            } else if (element.isJsonArray()) {
                list.add(toList(element.getAsJsonArray()));
            } else if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    list.add(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    list.add(primitive.getAsNumber());
                } else {
                    list.add(primitive.getAsString());
                }
            }
        }
        return list;
    }

    private static Map<String, Object> toMap(JsonObject object) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement element = entry.getValue();
            if (element == null || element.isJsonNull()) {
                map.put(entry.getKey(), null);
            } else if (element.isJsonObject()) {
                map.put(entry.getKey(), toMap(element.getAsJsonObject()));
            } else if (element.isJsonArray()) {
                map.put(entry.getKey(), toList(element.getAsJsonArray()));
            } else if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    map.put(entry.getKey(), primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    map.put(entry.getKey(), primitive.getAsNumber());
                } else {
                    map.put(entry.getKey(), primitive.getAsString());
                }
            }
        }
        return map;
    }

    private static JsonElement toJson(ConfigNode node) {
        if (!node.children().isEmpty()) {
            JsonObject object = new JsonObject();
            for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
                JsonElement child = toJson(entry.getValue());
                if (child != null) {
                    object.add(entry.getKey(), child);
                }
            }
            return object;
        }
        Object value = node.getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        }
        if (value instanceof List) {
            return toJsonArray((List<?>) value);
        }
        if (value instanceof Map) {
            return toJsonObject((Map<?, ?>) value);
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static JsonArray toJsonArray(List<?> list) {
        JsonArray array = new JsonArray();
        for (Object item : list) {
            array.add(toJsonValue(item));
        }
        return array;
    }

    private static JsonObject toJsonObject(Map<?, ?> map) {
        JsonObject object = new JsonObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            JsonElement value = toJsonValue(entry.getValue());
            if (value != null) {
                object.add(String.valueOf(entry.getKey()), value);
            }
        }
        return object;
    }

    private static JsonElement toJsonValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        }
        if (value instanceof List) {
            return toJsonArray((List<?>) value);
        }
        if (value instanceof Map) {
            return toJsonObject((Map<?, ?>) value);
        }
        return new JsonPrimitive(String.valueOf(value));
    }
}
