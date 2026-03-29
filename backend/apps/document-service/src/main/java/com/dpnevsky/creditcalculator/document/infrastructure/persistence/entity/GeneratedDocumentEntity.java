package com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_documents")
public class GeneratedDocumentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(name = "format", nullable = false, length = 32)
    private String format;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    protected GeneratedDocumentEntity() {
        // for JPA
    }

    public GeneratedDocumentEntity(
            UUID id,
            UUID requestId,
            UUID applicationId,
            String documentType,
            String format,
            String fileName,
            String mimeType,
            String storageKey,
            String status,
            OffsetDateTime generatedAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.applicationId = applicationId;
        this.documentType = documentType;
        this.format = format;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.status = status;
        this.generatedAt = generatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFormat() {
        return format;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }
}