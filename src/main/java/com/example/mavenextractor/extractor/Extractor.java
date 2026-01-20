package com.example.mavenextractor.extractor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts archive files (ZIP, JAR, TAR.GZ) to a directory.
 */
public class Extractor {

    private static final Logger logger = LoggerFactory.getLogger(Extractor.class);

    /**
     * Extracts an archive file to the specified output directory.
     * Supports ZIP, JAR, and TAR.GZ formats.
     *
     * @param archivePath the path to the archive file
     * @param outputDir the output directory
     * @return true if extraction succeeded, false otherwise
     */
    public static boolean extractArchive(Path archivePath, Path outputDir) {
        try {
            Files.createDirectories(outputDir);

            String fileName = archivePath.getFileName().toString().toLowerCase();

            if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                return extractZipArchive(archivePath, outputDir);
            } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                return extractTarGzArchive(archivePath, outputDir);
            } else {
                logger.error("Unsupported file format: {}", archivePath);
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to extract archive: {}", archivePath, e);
            return false;
        }
    }

    /**
     * Extracts a ZIP or JAR archive.
     */
    private static boolean extractZipArchive(Path zipPath, Path outputDir) throws IOException {
        logger.debug("Extracting ZIP archive: {}", zipPath);

        try (var zipFile = new ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = outputDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // Ensure parent directories exist
                    Files.createDirectories(entryPath.getParent());

                    try (var is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            return true;
        }
    }

    /**
     * Extracts a TAR.GZ archive.
     */
    private static boolean extractTarGzArchive(Path tarGzPath, Path outputDir) {
        logger.debug("Extracting TAR.GZ archive: {}", tarGzPath);

        try (var fi = new FileInputStream(tarGzPath.toFile());
             var bi = new BufferedInputStream(fi);
             var gzi = new GzipCompressorInputStream(bi);
             var ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path entryPath = outputDir.resolve(entry.getName());
                Files.createDirectories(entryPath.getParent());

                Files.copy(ti, entryPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to extract TAR.GZ archive", e);
            return false;
        }
    }

    /**
     * Copies a directory recursively.
     *
     * @param source the source directory
     * @param target the target directory
     * @return true if copy succeeded, false otherwise
     */
    public static boolean copyDirectory(Path source, Path target) {
        try {
            if (Files.exists(target)) {
                deleteDirectory(target);
            }

            Files.walk(source)
                 .forEach(sourcePath -> {
                     try {
                         Path targetPath = target.resolve(source.relativize(sourcePath));
                         Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                     } catch (IOException e) {
                         logger.error("Failed to copy file: {}", sourcePath, e);
                     }
                 });

            return true;
        } catch (IOException e) {
            logger.error("Failed to copy directory: {} -> {}", source, target, e);
            return false;
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory the directory to delete
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                 .sorted((a, b) -> -a.compareTo(b)) // Reverse order for deletion
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         logger.error("Failed to delete: {}", path, e);
                     }
                 });
        }
    }
}
