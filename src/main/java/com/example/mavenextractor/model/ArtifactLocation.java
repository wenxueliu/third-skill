package com.example.mavenextractor.model;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents the location of binary and source JAR files.
 * Uses JDK 21 record for immutable data carrier.
 */
public record ArtifactLocation(
    Optional<Path> binaryPath,
    Optional<Path> sourcePath
) {
    /**
     * Creates an ArtifactLocation with no files found.
     */
    public static ArtifactLocation notFound() {
        return new ArtifactLocation(Optional.empty(), Optional.empty());
    }

    /**
     * Creates an ArtifactLocation with only binary.
     */
    public static ArtifactLocation binaryOnly(Path binaryPath) {
        return new ArtifactLocation(Optional.of(binaryPath), Optional.empty());
    }

    /**
     * Creates an ArtifactLocation with only source.
     */
    public static ArtifactLocation sourceOnly(Path sourcePath) {
        return new ArtifactLocation(Optional.empty(), Optional.of(sourcePath));
    }

    /**
     * Creates an ArtifactLocation with both binary and source.
     */
    public static ArtifactLocation both(Path binaryPath, Path sourcePath) {
        return new ArtifactLocation(Optional.of(binaryPath), Optional.of(sourcePath));
    }

    /**
     * Returns true if source is available.
     */
    public boolean hasSource() {
        return sourcePath.isPresent();
    }

    /**
     * Returns true if binary is available.
     */
    public boolean hasBinary() {
        return binaryPath.isPresent();
    }
}
