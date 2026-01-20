package com.example.mavenextractor.decompiler;

import com.example.mavenextractor.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for the Fernflower Java decompiler (java-decompiler.jar).
 */
public class DecompilerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(DecompilerWrapper.class);

    private final Path decompilerPath;

    public DecompilerWrapper(Path decompilerPath) {
        this.decompilerPath = decompilerPath;
    }

    /**
     * Decompiles a JAR file to the specified output directory.
     *
     * @param jarPath the path to the JAR file to decompile
     * @param outputDir the output directory for decompiled sources
     * @return true if decompilation succeeded, false otherwise
     */
    public boolean decompile(Path jarPath, Path outputDir) {
        if (!Files.exists(decompilerPath)) {
            logger.error("Decompiler not found: {}", decompilerPath);
            return false;
        }

        try {
            Files.createDirectories(outputDir);

            // Build command: java -jar decompiler.jar -hes=0 -hdc=0 jarPath outputDir
            List<String> cmd = List.of(
                "java",
                "-jar",
                decompilerPath.toString(),
                "-hes=0",  // Hide empty super
                "-hdc=0",  // Hide default constructor
                jarPath.toString(),
                outputDir.toString()
            );

            logger.debug("Decompiling: {} -> {}", jarPath, outputDir);

            var result = ProcessExecutor.execute(cmd, null, java.time.Duration.ofMinutes(2));

            if (result.isSuccess()) {
                logger.debug("Decompilation succeeded: {}", jarPath);
                return true;
            } else {
                logger.warn("Decompilation failed for {}: {}", jarPath, result.stderr());
                return false;
            }

        } catch (IOException | InterruptedException | TimeoutException e) {
            logger.error("Decompilation error for: {}", jarPath, e);
            return false;
        }
    }

    /**
     * Checks if the decompiler is available.
     *
     * @return true if the decompiler JAR file exists
     */
    public boolean isAvailable() {
        return Files.exists(decompilerPath) && Files.isRegularFile(decompilerPath);
    }
}
