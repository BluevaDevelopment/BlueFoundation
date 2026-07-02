package net.blueva.foundation.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory configuration tree shared by every format codec. */
public class ConfigDocument {

    private final ConfigNode root = new ConfigNode();

    public ConfigNode root() {
        return root;
    }

    public boolean isEmpty() {
        return root.children().isEmpty();
    }

    public boolean contains(String path) {
        return node(path) != null;
    }

    public Object get(String path) {
        ConfigNode node = node(path);
        return node == null ? null : node.getValue();
    }

    public ConfigSection section(String path) {
        return node(path) == null ? null : new ConfigSection(this, path);
    }

    public ConfigSection sectionOrCreate(String path) {
        nodeOrCreate(path);
        return new ConfigSection(this, path);
    }

    public ConfigSection rootSection() {
        return new ConfigSection(this, "");
    }

    public void set(String path, Object value) {
        nodeOrCreate(path).setValue(value);
    }

    public boolean remove(String path) {
        String[] parts = ConfigPath.parts(path);
        if (parts.length == 0) {
            return false;
        }
        ConfigNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            parent = parent.child(parts[i]);
            if (parent == null) {
                return false;
            }
        }
        return parent.removeChild(parts[parts.length - 1]) != null;
    }

    public ConfigNode node(String path) {
        String[] parts = parts(path);
        ConfigNode current = root;
        for (String part : parts) {
            current = current.child(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public ConfigNode nodeOrCreate(String path) {
        String[] parts = parts(path);
        ConfigNode current = root;
        for (String part : parts) {
            current = current.childOrCreate(part);
        }
        return current;
    }

    public void putNode(String path, ConfigNode node) {
        String[] parts = ConfigPath.parts(path);
        if (parts.length == 0) {
            return;
        }
        ConfigNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            parent = parent.childOrCreate(parts[i]);
        }
        parent.putChild(parts[parts.length - 1], node);
    }

    public Map<String, ConfigNode> leaves() {
        LinkedHashMap<String, ConfigNode> leaves = new LinkedHashMap<>();
        collectLeaves("", root, leaves);
        return leaves;
    }

    public Map<String, ConfigNode> nodes() {
        LinkedHashMap<String, ConfigNode> nodes = new LinkedHashMap<>();
        collectNodes("", root, nodes);
        return nodes;
    }

    public ConfigDocument copy() {
        ConfigDocument copy = new ConfigDocument();
        for (Map.Entry<String, ConfigNode> entry : root.children().entrySet()) {
            copy.root.putChild(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    private static void collectLeaves(String path, ConfigNode node, Map<String, ConfigNode> leaves) {
        if (node.getValue() != null || node.children().isEmpty()) {
            if (!path.isEmpty()) {
                leaves.put(path, node);
            }
            return;
        }
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            collectLeaves(ConfigPath.joinLiteral(path, entry.getKey()), entry.getValue(), leaves);
        }
    }

    private static void collectNodes(String path, ConfigNode node, Map<String, ConfigNode> nodes) {
        if (!path.isEmpty()) {
            nodes.put(path, node);
        }
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            collectNodes(ConfigPath.joinLiteral(path, entry.getKey()), entry.getValue(), nodes);
        }
    }

    private static String[] parts(String path) {
        return ConfigPath.parts(path);
    }
}
