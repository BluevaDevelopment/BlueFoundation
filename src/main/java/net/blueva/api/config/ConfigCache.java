package net.blueva.api.config;

import java.util.LinkedHashMap;
import java.util.Map;

final class ConfigCache {
    private static final String HEADER = "blueapi_config_cache=2";
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    Entry get(String path) {
        return entries.get(path);
    }

    void put(String path, ConfigNode defaultNode, ConfigNode localNode) {
        entries.put(path, new Entry(
                hash(defaultNode == null ? null : defaultNode.getValue()),
                hash(localNode == null ? null : localNode.getValue()),
                commentHash(defaultNode),
                commentHash(localNode)
        ));
    }

    void putAdoptedCustom(String path, ConfigNode defaultNode, ConfigNode localNode) {
        String localHash = hash(localNode == null ? null : localNode.getValue());
        entries.put(path, new Entry(
                "adopted-custom:" + localHash,
                localHash,
                commentHash(localNode),
                commentHash(localNode)
        ));
    }

    String write() {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER).append('\n');
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            builder.append(escape(entry.getKey()))
                    .append('|')
                    .append(entry.getValue().defaultHash)
                    .append('|')
                    .append(entry.getValue().localHash)
                    .append('|')
                    .append(entry.getValue().defaultCommentHash)
                    .append('|')
                    .append(entry.getValue().localCommentHash)
                    .append('\n');
        }
        return builder.toString();
    }

    static ConfigCache read(String text) {
        ConfigCache cache = new ConfigCache();
        if (text == null) {
            return cache;
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            if (line.startsWith("blueapi_config_cache=") || line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 5);
            if (parts.length == 3) {
                cache.entries.put(unescape(parts[0]), new Entry(parts[1], parts[2], "", ""));
            } else if (parts.length == 5) {
                cache.entries.put(unescape(parts[0]), new Entry(parts[1], parts[2], parts[3], parts[4]));
            }
        }
        return cache;
    }

    static String hash(Object value) {
        return sha1(ConfigValues.canonical(value));
    }

    static String commentHash(ConfigNode node) {
        if (node == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String comment : node.comments()) {
            builder.append(comment).append('\n');
        }
        builder.append('|').append(node.getInlineComment() == null ? "" : node.getInlineComment());
        return sha1(builder.toString());
    }

    private static String sha1(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                if (c == 'p') {
                    builder.append('|');
                } else if (c == 'n') {
                    builder.append('\n');
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
        return builder.toString();
    }

    static final class Entry {
        final String defaultHash;
        final String localHash;
        final String defaultCommentHash;
        final String localCommentHash;

        private Entry(String defaultHash, String localHash, String defaultCommentHash, String localCommentHash) {
            this.defaultHash = defaultHash;
            this.localHash = localHash;
            this.defaultCommentHash = defaultCommentHash;
            this.localCommentHash = localCommentHash;
        }
    }
}
