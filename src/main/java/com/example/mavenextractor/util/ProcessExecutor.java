package com.example.mavenextractor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for executing external processes.
 * Uses JDK 21 modern features for process handling.
 */
public class ProcessExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessExecutor.class);

    /**
     * Result of a process execution.
     */
    public record ProcessResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut
    ) {
        public boolean isSuccess() {
            return exitCode == 0 && !timedOut;
        }
    }

    /**
     * Executes a command and waits for completion.
     *
     * @param command the command and its arguments
     * @param workingDir the working directory (null for current directory)
     * @param timeout the timeout duration (null for no timeout)
     * @return the process result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     * @throws TimeoutException if the process times out
     */
    public static ProcessResult execute(
        List<String> command,
        Path workingDir,
        Duration timeout
    ) throws IOException, InterruptedException, TimeoutException {

        logger.debug("Executing command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }

        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Use StringBuilder to capture output in threads
        StringBuilder stdoutCapture = new StringBuilder();
        StringBuilder stderrCapture = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (var br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    stdoutCapture.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.error("Error reading stdout", e);
            }
        });

        Thread stderrReader = new Thread(() -> {
            try (var br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    stderrCapture.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.error("Error reading stderr", e);
            }
        });

        stdoutReader.start();
        stderrReader.start();

        boolean timedOut = false;
        if (timeout != null) {
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                timedOut = true;
                throw new TimeoutException("Process timed out after " + timeout);
            }
        } else {
            process.waitFor();
        }

        stdoutReader.join(1000);
        stderrReader.join(1000);

        String stdout = stdoutCapture.toString();
        String stderr = stderrCapture.toString();

        return new ProcessResult(process.exitValue(), stdout, stderr, timedOut);
    }

    /**
     * Executes a command with a default timeout of 2 minutes.
     */
    public static ProcessResult execute(List<String> command, Path workingDir)
        throws IOException, InterruptedException, TimeoutException {
        return execute(command, workingDir, Duration.ofMinutes(2));
    }

    /**
     * Executes a command and returns only the exit code.
     */
    public static int executeSimple(List<String> command, Path workingDir, Duration timeout)
        throws IOException, InterruptedException, TimeoutException {

        ProcessResult result = execute(command, workingDir, timeout);
        return result.exitCode();
    }

    /**
     * Checks if a command is available by executing it with --version flag.
     */
    public static boolean isCommandAvailable(String command, String versionFlag) {
        try {
            var cmd = List.of(command, versionFlag);
            var result = execute(cmd, null, Duration.ofSeconds(10));
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Finds the full path of a command using 'which' (Unix) or 'where' (Windows).
     */
    public static String findCommandPath(String command) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        String whichCommand = os.contains("win") ? "where" : "which";

        try {
            var pb = new ProcessBuilder(whichCommand, command);
            var process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                var output = new String(process.getInputStream().readAllBytes());
                return output.lines().findFirst().orElse(command);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to find command path for: {}", command, e);
        }

        return command;
    }
}
