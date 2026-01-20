package com.example.mavenextractor.model;

/**
 * Represents a Maven dependency with groupId, artifactId, and version.
 * Uses JDK 21 record for immutable data carrier.
 */
public record Dependency(
    String groupId,
    String artifactId,
    String version,
    String scope
) {
    /**
     * Returns a unique key for this dependency.
     */
    public String key() {
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Creates a Dependency with default compile scope.
     */
    public static Dependency of(String groupId, String artifactId, String version) {
        return new Dependency(groupId, artifactId, version, "compile");
    }

    /**
     * Creates a Dependency from a dependency tree line.
     * Format: "groupId:artifactId:jar:version:scope"
     */
    public static Dependency fromTreeLine(String line) {
        String[] parts = line.split(":");
        if (parts.length >= 5) {
            return new Dependency(parts[0], parts[1], parts[3], parts[4]);
        }
        throw new IllegalArgumentException("Invalid dependency tree line: " + line);
    }
}
