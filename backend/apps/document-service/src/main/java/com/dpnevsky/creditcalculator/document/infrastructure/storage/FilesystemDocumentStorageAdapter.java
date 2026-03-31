package com.dpnevsky.creditcalculator.document.infrastructure.storage;

import com.dpnevsky.creditcalculator.document.application.port.out.DocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

@Component
public class FilesystemDocumentStorageAdapter implements DocumentStoragePort {

    private final Path basePath;

    public FilesystemDocumentStorageAdapter(
            @Value("${app.document-storage.base-path:build/document-storage}") String basePath
    ) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public void save(SaveDocumentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.storageKey(), "storageKey must not be null");
        Objects.requireNonNull(command.content(), "content must not be null");

        Path targetPath = resolvePath(command.storageKey());

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(
                    targetPath,
                    command.content(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to save document to filesystem storage. storageKey=" + command.storageKey(),
                    ex
            );
        }
    }

    @Override
    public StoredDocument load(String storageKey) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");

        Path targetPath = resolvePath(storageKey);

        if (!Files.exists(targetPath)) {
            throw new IllegalStateException(
                    "Document not found in filesystem storage. storageKey=" + storageKey
            );
        }

        try {
            return new StoredDocument(Files.readAllBytes(targetPath));
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load document from filesystem storage. storageKey=" + storageKey,
                    ex
            );
        }
    }

    private Path resolvePath(String storageKey) {
        Path resolvedPath = basePath.resolve(storageKey).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid storageKey: " + storageKey);
        }

        return resolvedPath;
    }
}