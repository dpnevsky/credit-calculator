package com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_documents")
public class ApplicationDocumentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ApplicationDocumentEntity() {
        // for JPA
    }

    public ApplicationDocumentEntity(
            UUID id,
            UUID applicationId,
            UUID requestId,
            UUID documentId,
            String documentType,
            String format,
            String fileName,
            String mimeType,
            String storageKey,
            String status,
            OffsetDateTime generatedAt,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.requestId = requestId;
        this.documentId = documentId;
        this.documentType = documentType;
        this.format = format;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.status = status;
        this.generatedAt = generatedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getDocumentId() {
        return documentId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}