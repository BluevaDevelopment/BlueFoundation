package net.blueva.foundation.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A node in a BlueFoundation configuration document. */
public class ConfigNode {

    private final LinkedHashMap<String, ConfigNode> children = new LinkedHashMap<>();
    private final List<String> comments = new ArrayList<>();
    private Object value;
    private String inlineComment;

    public boolean isSection() {
        return !children.isEmpty() || value == null;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = normalize(value);
        if (value != null) {
            children.clear();
        }
    }

    public Map<String, ConfigNode> children() {
        return Collections.unmodifiableMap(children);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ConfigNode child(String key) {
        return children.get(key);
    }

    public ConfigNode childOrCreate(String key) {
        ConfigNode child = children.get(key);
        if (child == null) {
            child = new ConfigNode();
            value = null;
            children.put(key, child);
        }
        return child;
    }

    public void putChild(String key, ConfigNode node) {
        if (key != null && node != null) {
            value = null;
            children.put(key, node);
        }
    }

    public ConfigNode removeChild(String key) {
        return key == null ? null : children.remove(key);
    }

    public List<String> comments() {
        return comments;
    }

    public ConfigNode comments(String... comments) {
        this.comments.clear();
        if (comments != null) {
            for (String comment : comments) {
                if (comment != null) {
                    this.comments.add(comment);
                }
            }
        }
        return this;
    }

    public String getInlineComment() {
        return inlineComment;
    }

    public void setInlineComment(String inlineComment) {
        this.inlineComment = inlineComment;
    }

    public ConfigNode inlineComment(String inlineComment) {
        this.inlineComment = inlineComment;
        return this;
    }

    ConfigNode copy() {
        ConfigNode copy = new ConfigNode();
        copy.value = copyValue(value);
        copy.inlineComment = inlineComment;
        copy.comments.addAll(comments);
        for (Map.Entry<String, ConfigNode> entry : children.entrySet()) {
            copy.children.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object normalize(Object value) {
        return copyValue(value);
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value) {
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                copy.add(copyValue(item));
            }
            return copy;
        }
        if (value instanceof Map) {
            LinkedHashMap<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                copy.put(entry.getKey(), copyValue(entry.getValue()));
            }
            return copy;
        }
        return value;
    }
}
