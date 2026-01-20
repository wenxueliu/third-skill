package com.example.mavenextractor;

import com.example.mavenextractor.config.Config;
import com.example.mavenextractor.detector.MavenDetector;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Callable;

import static com.example.mavenextractor.config.Config.THIRD_DIR;

/**
 * Main entry point for the Maven Dependency Extractor.
 * Uses picocli for command-line argument parsing.
 */
@Command(
    name = "maven-dependency-extractor",
    mixinStandardHelpOptions = true,
    version = "Maven Dependency Extractor 1.0",
    description = "Extract Maven project dependencies and their source code",
    footer = {
        "",
        "Examples:",
        "  # Basic usage (extract sources without decompilation)",
        "  java -jar maven-dependency-extractor.jar /path/to/project",
        "",
        "  # Use decompiler",
        "  java -jar maven-dependency-extractor.jar /path/to/project -d java-decompiler.jar",
        "",
        "  # Specify output directory",
        "  java -jar maven-dependency-extractor.jar /path/to/project -o custom_output",
        "",
        "  # Extract only direct dependencies (exclude transitive)",
        "  java -jar maven-dependency-extractor.jar /path/to/project --direct-only",
        "",
        "",
        "Environment Variables:",
        "  MAVEN_HOME  Maven installation directory",
        "  THIRD_DIR   Output directory name (default: third, relative to project dir)"
    }
)
public class Main implements Callable<Integer> {

    @Option(
        names = {"-d", "--decompiler"},
        paramLabel = "DECOMPILER",
        description = "Path to Java decompiler (java-decompiler.jar)"
    )
    private String decompilerPath;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "DIR",
        description = "Output directory name, relative to project dir unless absolute (default: ${DEFAULT-VALUE}, can be overridden by THIRD_DIR env var)"
    )
    private String outputDir;

    @Option(
        names = {"--direct-only"},
        description = "Extract only direct dependencies, excluding transitive dependencies"
    )
    private boolean directOnly = false;

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "PROJECT_DIR",
        description = "Maven project directory (containing pom.xml)"
    )
    private String projectDir;

    @Override
    public Integer call() throws Exception {
        try {
            // Validate project directory
            Path projectPath = Paths.get(projectDir);
            if (!Files.exists(projectPath)) {
                System.err.println("Error: Project directory does not exist: " + projectDir);
                return 1;
            }

            Path pomXml = projectPath.resolve("pom.xml");
            if (!Files.exists(pomXml)) {
                System.err.println("Error: pom.xml not found in: " + projectDir);
                return 1;
            }

            // Detect Maven
            var mavenCommand = MavenDetector.findMaven();
            if (mavenCommand.isEmpty()) {
                System.err.println("Error: Maven not found. Please install Maven or set MAVEN_HOME environment variable");
                return 1;
            }

            // Resolve decompiler path if provided
            Path decompilerPathResolved = null;
            if (decompilerPath != null) {
                decompilerPathResolved = Paths.get(decompilerPath);
                if (!Files.exists(decompilerPathResolved)) {
                    System.err.println("Warning: Decompiler JAR not found: " + decompilerPath);
                    decompilerPathResolved = null;
                }
            }

            // Resolve output directory (relative to project dir if not absolute)
            Path outputPath;
            if (Objects.isNull(outputDir)) {
                // If outputDir is relative, resolve it against project directory
                outputPath = projectPath.resolve(THIRD_DIR);
            } else {
                outputPath = Paths.get(outputDir);
            }

            // Create extractor and run
            MavenDependencyExtractor extractor = new MavenDependencyExtractor(
                projectPath,
                mavenCommand.get(),
                outputPath,
                decompilerPathResolved,
                directOnly
            );

            extractor.run();

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
