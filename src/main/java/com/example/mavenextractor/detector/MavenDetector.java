package com.example.mavenextractor.detector;

import com.example.mavenextractor.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Detects the Maven installation by checking MAVEN_HOME environment variable
 * or searching in the PATH environment variable.
 */
public class MavenDetector {

    private static final Logger logger = LoggerFactory.getLogger(MavenDetector.class);

    /**
     * Finds the Maven command path.
     * Priority: MAVEN_HOME environment variable > PATH environment variable.
     *
     * @return the Maven command path, or empty if not found
     */
    public static Optional<String> findMaven() {
        // 1. Check MAVEN_HOME environment variable
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.isBlank()) {
            Path mavenCmd = Paths.get(mavenHome, "bin", isWindows() ? "mvn.cmd" : "mvn");
            if (Files.exists(mavenCmd) && isValidMaven(mavenCmd.toString())) {
                logger.info("Found Maven (from MAVEN_HOME): {}", mavenCmd);
                return Optional.of(mavenCmd.toString());
            }
        }

        // 2. Check PATH environment variable
        String mavenCmd = isWindows() ? "mvn.cmd" : "mvn";
        if (isValidMaven(mavenCmd)) {
            try {
                String fullPath = ProcessExecutor.findCommandPath(mavenCmd);
                logger.info("Found Maven (from PATH): {}", fullPath);
                return Optional.of(mavenCmd); // Return just the command name
            } catch (IOException | InterruptedException e) {
                logger.warn("Failed to find Maven path", e);
            }
        }

        logger.error("Maven not found. Please install Maven or set MAVEN_HOME environment variable");
        return Optional.empty();
    }

    /**
     * Validates if the given command is a valid Maven installation.
     *
     * @param cmd the Maven command to validate
     * @return true if the command is a valid Maven installation
     */
    private static boolean isValidMaven(String cmd) {
        try {
            var result = ProcessExecutor.execute(
                java.util.List.of(cmd, "--version"),
                null,
                java.time.Duration.ofSeconds(10)
            );
            return result.isSuccess() && result.stdout().contains("Apache Maven");
        } catch (IOException | InterruptedException | TimeoutException e) {
            logger.debug("Maven validation failed for command: {}", cmd, e);
            return false;
        }
    }

    /**
     * Checks if the operating system is Windows.
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}
