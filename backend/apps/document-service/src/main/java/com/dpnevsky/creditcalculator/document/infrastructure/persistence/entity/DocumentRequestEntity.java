package com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_requests")
public class DocumentRequestEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formats_json", nullable = false, columnDefinition = "jsonb")
    private List<String> formatsJson;

    @Column(name = "template_code", nullable = false, length = 128)
    private String templateCode;

    @Column(name = "template_version", nullable = false, length = 64)
    private String templateVersion;

    @Column(name = "requested_by_user_id", nullable = false, length = 128)
    private String requestedByUserId;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "render_context_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> renderContextJson;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DocumentRequestEntity() {
        // for JPA
    }

    public DocumentRequestEntity(
            UUID id,
            UUID requestId,
            UUID applicationId,
            String documentType,
            List<String> formatsJson,
            String templateCode,
            String templateVersion,
            String requestedByUserId,
            OffsetDateTime requestedAt,
            Map<String, Object> renderContextJson,
            String status,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.applicationId = applicationId;
        this.documentType = documentType;
        this.formatsJson = formatsJson;
        this.templateCode = templateCode;
        this.templateVersion = templateVersion;
        this.requestedByUserId = requestedByUserId;
        this.requestedAt = requestedAt;
        this.renderContextJson = renderContextJson;
        this.status = status;
        this.createdAt = createdAt;
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

    public List<String> getFormatsJson() {
        return formatsJson;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public Map<String, Object> getRenderContextJson() {
        return renderContextJson;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}