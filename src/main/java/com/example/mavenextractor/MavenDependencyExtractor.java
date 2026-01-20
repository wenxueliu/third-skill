package com.example.mavenextractor;

import com.example.mavenextractor.config.Config;
import com.example.mavenextractor.decompiler.DecompilerWrapper;
import com.example.mavenextractor.extractor.Extractor;
import com.example.mavenextractor.locator.ArtifactLocator;
import com.example.mavenextractor.model.ArtifactLocation;
import com.example.mavenextractor.model.Dependency;
import com.example.mavenextractor.model.ExtractionStats;
import com.example.mavenextractor.parser.DependencyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Main orchestrator for Maven dependency extraction.
 * Coordinates parsing, downloading, extracting, and decompiling dependencies.
 */
public class MavenDependencyExtractor {

    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyExtractor.class);

    private final Path projectDir;
    private final Path outputDir;
    private final String mavenCommand;
    private final DecompilerWrapper decompiler;
    private final boolean directDependenciesOnly;

    private final DependencyParser parser;
    private final ArtifactLocator locator;

    /**
     * Creates a new MavenDependencyExtractor.
     *
     * @param projectDir the Maven project directory
     * @param mavenCommand the Maven command path
     * @param outputDir the output directory for extracted sources
     * @param decompilerPath the path to the decompiler JAR (optional)
     */
    public MavenDependencyExtractor(
        Path projectDir,
        String mavenCommand,
        Path outputDir,
        Path decompilerPath
    ) {
        this(projectDir, mavenCommand, outputDir, decompilerPath, false);
    }

    /**
     * Creates a new MavenDependencyExtractor with option to extract only direct dependencies.
     *
     * @param projectDir the Maven project directory
     * @param mavenCommand the Maven command path
     * @param outputDir the output directory for extracted sources
     * @param decompilerPath the path to the decompiler JAR (optional)
     * @param directDependenciesOnly if true, only extract direct dependencies
     */
    public MavenDependencyExtractor(
        Path projectDir,
        String mavenCommand,
        Path outputDir,
        Path decompilerPath,
        boolean directDependenciesOnly
    ) {
        this.projectDir = projectDir;
        this.mavenCommand = mavenCommand;
        this.outputDir = outputDir;
        this.decompiler = decompilerPath != null ? new DecompilerWrapper(decompilerPath) : null;
        this.directDependenciesOnly = directDependenciesOnly;

        this.parser = new DependencyParser(mavenCommand, projectDir, directDependenciesOnly);
        this.locator = new ArtifactLocator();
    }

    /**
     * Executes the extraction process.
     */
    public void run() {
        logger.info("============================================================");
        logger.info("Maven Dependency Extractor");
        logger.info("============================================================");
        logger.info("Project Directory: {}", projectDir);
        logger.info("Output Directory: {}", outputDir);
        logger.info("Maven Command: {}", mavenCommand);
        logger.info("Direct Dependencies Only: {}", directDependenciesOnly);
        logger.info("Decompiler: {}",
            decompiler != null && decompiler.isAvailable() ? "Available" : "Not available");
        logger.info("============================================================");

        try {
            // 1. Get dependency tree
            List<Dependency> dependencies = parser.getDependencyTree();
            if (dependencies.isEmpty()) {
                logger.warn("No dependencies found");
                return;
            }

            // 2. Download sources
            parser.downloadSources(dependencies);

            // 3. Process each dependency
            logger.info("Processing dependencies...");
            Files.createDirectories(outputDir);

            ExtractionStats stats = ExtractionStats.initial(dependencies.size());

            for (int i = 0; i < dependencies.size(); i++) {
                Dependency dep = dependencies.get(i);
                logger.info("\n[{}/{}] Processing: {}", i + 1, stats.total(), dep.key());

                stats = processDependency(dep, i, stats);
            }

            // 4. Print statistics
            printStatistics(stats);

        } catch (Exception e) {
            logger.error("Extraction failed", e);
            throw new RuntimeException("Extraction failed", e);
        }
    }

    /**
     * Processes a single dependency.
     */
    private ExtractionStats processDependency(
        Dependency dep,
        int index,
        ExtractionStats stats
    ) {
        // Find artifacts
        ArtifactLocation location = locator.findArtifacts(dep);

        if (location.hasSource()) {
            // Source JAR available - extract it
            Path sourcePath = location.sourcePath().get();
            logger.info("  ✓ Found source JAR: {}", sourcePath.getFileName());

            Path artifactDir = outputDir.resolve(dep.artifactId());
            if (Extractor.extractArchive(sourcePath, artifactDir)) {
                logger.info("  ✓ Source extracted to: {}", artifactDir);
                return stats.incrementSourceExtracted();
            } else {
                return stats.incrementFailed();
            }
        } else if (location.hasBinary()) {
            // Only binary JAR available - try decompilation
            logger.info("  ⚠ Source JAR not found, using binary JAR");

            if (decompiler != null && decompiler.isAvailable()) {
                Path binaryPath = location.binaryPath().get();
                logger.info("  → Decompiling: {}", binaryPath.getFileName());

                Path artifactDir = outputDir.resolve(dep.artifactId());
                Path tempDir = artifactDir.resolveSibling(artifactDir.getFileName() + "_temp");

                if (decompiler.decompile(binaryPath, tempDir)) {
                    // Move decompiled result to target directory
                    if (Files.exists(tempDir)) {
                        try {
                            // Find actual output directory (Fernflower may create subdirectories)
                            try (var stream = Files.list(tempDir)) {
                                Optional<Path> firstItem = stream.findFirst();
                                if (firstItem.isPresent()) {
                                    Path srcPath = firstItem.get();
                                    Extractor.copyDirectory(srcPath, artifactDir);
                                    Extractor.deleteDirectory(tempDir);
                                    logger.info("  ✓ Decompilation completed: {}", artifactDir);
                                    return stats.incrementDecompiled();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to organize decompiled output", e);
                        }
                    }
                }
                return stats.incrementFailed();
            } else {
                logger.info("  ⚠ Skipped (decompiler not configured)");
                return stats.incrementSkipped();
            }
        } else {
            logger.info("  ✗ Artifact not found (binary and source JARs not available)");
            return stats.incrementFailed();
        }
    }

    /**
     * Prints extraction statistics.
     */
    private void printStatistics(ExtractionStats stats) {
        logger.info("============================================================");
        logger.info("Extraction Complete!");
        logger.info("============================================================");
        logger.info("Total Dependencies: {}", stats.total());
        logger.info("Sources Extracted: {}", stats.sourceExtracted());
        logger.info("Decompiled: {}", stats.decompiled());
        logger.info("Skipped: {}", stats.skipped());
        logger.info("Failed: {}", stats.failed());
        logger.info("Output Directory: {}", outputDir.toAbsolutePath());
        logger.info("============================================================");
    }
}
