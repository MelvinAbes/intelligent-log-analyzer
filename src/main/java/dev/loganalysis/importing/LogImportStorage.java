package dev.loganalysis.importing;

import dev.loganalysis.config.AnalysisProperties;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LogImportStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".log", ".txt", ".jsonl");

    private final Path root;

    public LogImportStorage(AnalysisProperties properties) {
        root = properties.imports().storagePath().toAbsolutePath().normalize();
    }

    @PostConstruct
    void initialize() throws IOException {
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        validate(file);
        String storageKey = UUID.randomUUID() + "/source.log";
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (var source = file.getInputStream()) {
                Files.copy(source, target);
            }
            return storageKey;
        } catch (IOException error) {
            deleteQuietly(storageKey);
            throw new InvalidImportException(
                    "import_storage_failed", "Log import could not be stored.");
        }
    }

    public BufferedReader open(String storageKey) throws IOException {
        return Files.newBufferedReader(resolve(storageKey), StandardCharsets.UTF_8);
    }

    public void delete(String storageKey) throws IOException {
        Path source = resolve(storageKey);
        Files.deleteIfExists(source);
        Files.deleteIfExists(source.getParent());
    }

    public void deleteQuietly(String storageKey) {
        try {
            delete(storageKey);
        } catch (IOException ignored) {
            // The terminal cleanup path cannot safely recover from a second filesystem failure.
        }
    }

    private void validate(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new InvalidImportException("empty_import", "Log import must not be empty.");
        }
        if (filename == null
                || filename.isBlank()
                || filename.length() > 255
                || filename.contains("/")
                || filename.contains("\\")) {
            throw new InvalidImportException("invalid_filename", "Log import filename is invalid.");
        }
        String lower = filename.toLowerCase(java.util.Locale.ROOT);
        if (ALLOWED_EXTENSIONS.stream().noneMatch(lower::endsWith)) {
            throw new InvalidImportException(
                    "unsupported_extension",
                    "Supported import extensions are .log, .txt, and .jsonl.");
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new InvalidImportException(
                    "invalid_storage_key", "Log import storage key is invalid.");
        }
        return resolved;
    }
}
