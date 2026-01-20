package com.example.mavenextractor.parser;

import com.example.mavenextractor.config.Config;
import com.example.mavenextractor.model.Dependency;
import com.example.mavenextractor.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Maven dependencies from a project using Maven dependency tree.
 */
public class DependencyParser {

    private static final Logger logger = LoggerFactory.getLogger(DependencyParser.class);

    // Pattern to match: groupId:artifactId:type[:classifier]:version:scope
    // Maven dependency:tree format: groupId:artifactId:jar[:classifier]:version:scope
    private static final Pattern DEPENDENCY_PATTERN =
        Pattern.compile("([\\w.-]+):([\\w.-]+):(jar|war|ear|pom|aar)(?::([\\w.-]+))?:([\\w.-]+):([\\w.-]+)");

    private final String mavenCommand;
    private final Path projectDir;
    private final boolean directOnly;

    public DependencyParser(String mavenCommand, Path projectDir) {
        this(mavenCommand, projectDir, false);
    }

    public DependencyParser(String mavenCommand, Path projectDir, boolean directOnly) {
        this.mavenCommand = mavenCommand;
        this.projectDir = projectDir;
        this.directOnly = directOnly;
    }

    /**
     * Gets the dependency tree by running 'mvn dependency:tree'.
     *
     * @return list of dependencies
     * @throws IOException if pom.xml not found
     */
    public List<Dependency> getDependencyTree() throws IOException {
        Path pomFile = projectDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            throw new IOException("pom.xml not found: " + pomFile);
        }

        logger.info("Parsing dependency tree: {}", pomFile);

        // Execute mvn dependency:tree
        List<String> cmd = List.of(
            mavenCommand,
            "dependency:tree",
            "-DoutputFile=" + Config.DEPENDENCY_TREE_FILE,
            "-DappendOutput=false"
        );

        try {
            var result = ProcessExecutor.execute(cmd, projectDir, java.time.Duration.ofMinutes(5));

            if (result.exitCode() != 0) {
                throw new IOException("Maven command failed: " + result.stderr());
            }

            // Parse the dependency tree file
            Path treeFile = projectDir.resolve(Config.DEPENDENCY_TREE_FILE);
            if (!Files.exists(treeFile)) {
                throw new IOException("Dependency tree file not found: " + treeFile);
            }

            var dependencies = parseDependencyTreeFile(treeFile);
            Files.deleteIfExists(treeFile);
            return dependencies;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Maven command interrupted", e);
        } catch (TimeoutException e) {
            throw new IOException("Maven command timed out", e);
        }
    }

    /**
     * Parses the Maven dependency tree file.
     * Format: groupId:artifactId:type[:classifier]:version:scope
     * Indentation level determines dependency depth.
     */
    private List<Dependency> parseDependencyTreeFile(Path treeFile) {
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean firstProjectSkipped = false;

        try {
            var lines = Files.readAllLines(treeFile);
            for (String line : lines) {
                // Calculate indentation level based on tree structure
                // Maven dependency tree format:
                // Level 0: com.example:project:jar:1.0
                // Level 1: +- dependency
                // Level 2: |  +- dependency
                // Level 3: |  |  +- dependency
                // Each level is marked by "|  " prefix

                int indentLevel = 0;
                int originalLength = line.length();

                // Count the number of "|  " patterns to determine depth
                while (line.startsWith("|  ")) {
                    indentLevel++;
                    line = line.substring(3); // Remove "|  " prefix
                }

                // Remove any remaining tree markers ("+-", "\-", or just "+")
                if (line.startsWith("+-") || line.startsWith("\\-") || line.startsWith("+ ")) {
                    line = line.substring(3);
                } else if (line.startsWith("|")) {
                    // Handle cases where line ends with "|"
                    line = line.substring(1).trim();
                }

                Matcher matcher = DEPENDENCY_PATTERN.matcher(line);
                if (matcher.find()) {
                    String groupId = matcher.group(1);
                    String artifactId = matcher.group(2);
                    String type = matcher.group(3);
                    String classifier = matcher.group(4); // may be null
                    String version = matcher.group(5);
                    String scope = matcher.group(6);

                    String key = groupId + ":" + artifactId + ":" + version;

                    // Skip the first line (the project itself)
                    if (!firstProjectSkipped) {
                        firstProjectSkipped = true;
                        logger.info("Skipping project itself: {}", key);
                        continue;
                    }

                    // If directOnly is true, only include dependencies with indentLevel == 0
                    // After skipping project itself:
                    // indentLevel == 0 are direct dependencies
                    // indentLevel >= 1 are transitive dependencies
                    if (directOnly && indentLevel > 0) {
                        logger.debug("Skipping transitive dependency (level {}): {}",
                            indentLevel, key);
                        continue;
                    }

                    if (seen.add(key)) {
                        dependencies.add(new Dependency(groupId, artifactId, version,
                            scope != null ? scope : "compile"));
                    }
                }
            }
            logger.info("Found {} dependencies (directOnly: {})", dependencies.size(), directOnly);
        } catch (IOException e) {
            logger.error("Failed to read dependency tree file", e);
        }

        return dependencies;
    }

    /**
     * Downloads source JARs for all dependencies.
     */
    public void downloadSources(List<Dependency> dependencies) {
        logger.info("Downloading source JARs...");

        List<String> cmd = List.of(mavenCommand, "dependency:sources");

        try {
            var result = ProcessExecutor.execute(cmd, projectDir, java.time.Duration.ofMinutes(10));
            if (result.exitCode() == 0) {
                logger.info("Source JARs download completed");
            } else {
                logger.warn("Source JARs download failed: {}", result.stderr());
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            logger.warn("Failed to download sources", e);
        }
    }
}
