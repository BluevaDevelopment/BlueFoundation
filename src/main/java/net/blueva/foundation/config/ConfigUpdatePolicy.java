package net.blueva.foundation.config;

/**
 * Controls how a configuration file reacts when bundled defaults change.
 */
public enum ConfigUpdatePolicy {
    /**
     * Keep the file synchronized with bundled defaults while preserving custom values.
     */
    MERGE_DEFAULTS,

    /**
     * Copy bundled defaults only when the file is missing and never restore removed paths.
     */
    COPY_DEFAULTS_ONLY
}
