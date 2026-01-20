package com.example.mavenextractor.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for the Maven dependency extractor.
 * Supports environment variable overrides.
 */
public class Config {

    /**
     * Output directory (can be configured via THIRD_DIR environment variable).
     * Default is "third" relative to the project directory.
     */
    public static final Path THIRD_DIR = Paths.get(
        System.getenv().getOrDefault("THIRD_DIR", "third")
    );

    /**
     * Maven repository path (typically ~/.m2/repository).
     */
    public static final Path MAVEN_REPO = Paths.get(
        System.getProperty("user.home"),
        ".m2",
        "repository"
    );

    /**
     * Local cache directory for temporary files.
     */
    public static final Path CACHE_DIR = Paths.get(".maven_deps_cache");

    /**
     * Default decompiler jar file name.
     */
    public static final String DEFAULT_DECOMPILER = "java-decompiler.jar";

    /**
     * Dependency tree output file name.
     */
    public static final String DEPENDENCY_TREE_FILE = "dependency-tree.txt";

    private Config() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the Maven repository path as a string.
     */
    public static String getMavenRepoPath() {
        return MAVEN_REPO.toString();
    }

    /**
     * Gets the output directory path as a string.
     */
    public static String getOutputDirPath() {
        return THIRD_DIR.toString();
    }
}
