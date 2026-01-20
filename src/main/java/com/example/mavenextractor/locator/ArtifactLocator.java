package com.example.mavenextractor.locator;

import com.example.mavenextractor.config.Config;
import com.example.mavenextractor.model.ArtifactLocation;
import com.example.mavenextractor.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Locates Maven artifacts (binary and source JARs) in the local Maven repository.
 */
public class ArtifactLocator {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactLocator.class);

    private final Path repoPath;

    public ArtifactLocator() {
        this.repoPath = Config.MAVEN_REPO;
    }

    /**
     * Finds the binary and source JARs for a dependency.
     *
     * @param dependency the Maven dependency
     * @return the artifact location
     */
    public ArtifactLocation findArtifacts(Dependency dependency) {
        String groupId = dependency.groupId();
        String artifactId = dependency.artifactId();
        String version = dependency.version();

        // Convert groupId to path (e.g., com.example -> com/example)
        String groupPath = groupId.replace('.', '/');

        // Binary JAR path
        Path binaryJar = repoPath.resolve(groupPath)
                                  .resolve(artifactId)
                                  .resolve(version)
                                  .resolve(artifactId + "-" + version + ".jar");

        // Source JAR path
        Path sourceJar = repoPath.resolve(groupPath)
                                  .resolve(artifactId)
                                  .resolve(version)
                                  .resolve(artifactId + "-" + version + "-sources.jar");

        boolean binaryExists = Files.exists(binaryJar);
        boolean sourceExists = Files.exists(sourceJar);

        if (sourceExists && binaryExists) {
            return ArtifactLocation.both(binaryJar, sourceJar);
        } else if (sourceExists) {
            return ArtifactLocation.sourceOnly(sourceJar);
        } else if (binaryExists) {
            return ArtifactLocation.binaryOnly(binaryJar);
        } else {
            return ArtifactLocation.notFound();
        }
    }

    /**
     * Finds only the source JAR for a dependency.
     *
     * @param dependency the Maven dependency
     * @return the source JAR path, or empty if not found
     */
    public Optional<Path> findSourceJar(Dependency dependency) {
        var location = findArtifacts(dependency);
        return location.sourcePath();
    }

    /**
     * Finds only the binary JAR for a dependency.
     *
     * @param dependency the Maven dependency
     * @return the binary JAR path, or empty if not found
     */
    public Optional<Path> findBinaryJar(Dependency dependency) {
        var location = findArtifacts(dependency);
        return location.binaryPath();
    }
}
