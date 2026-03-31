package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.document.application.port.out.DocumentStoragePort;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DocumentGenerationService {

    private static final String GENERATED_STATUS = "GENERATED";

    private final DocumentStoragePort documentStoragePort;

    public DocumentGenerationService(DocumentStoragePort documentStoragePort) {
        this.documentStoragePort = documentStoragePort;
    }

    public DocumentGenerated generate(DocumentGenerationRequested request) {
        String format = request.formats().get(0);
        String fileExtension = resolveFileExtension(format);
        String mimeType = resolveMimeType(format);

        UUID documentId = UUID.randomUUID();
        String fileName = request.documentType().toLowerCase() + "-" + request.applicationId() + "." + fileExtension;
        String storageKey = "applications/" + request.applicationId() + "/" + fileName;
        OffsetDateTime generatedAt = OffsetDateTime.now();

        DocumentGenerated generatedDocument = new DocumentGenerated(
                request.requestId(),
                request.applicationId(),
                documentId,
                request.documentType(),
                format,
                fileName,
                mimeType,
                storageKey,
                GENERATED_STATUS,
                generatedAt
        );

        byte[] content = generateContent(request, generatedDocument);

        documentStoragePort.save(new DocumentStoragePort.SaveDocumentCommand(
                storageKey,
                content
        ));

        return generatedDocument;
    }

    private byte[] generateContent(DocumentGenerationRequested request, DocumentGenerated generatedDocument) {
        return switch (generatedDocument.format()) {
            case "PDF" -> generatePdfContent(request, generatedDocument);
            case "XML" -> generateXmlContent(request, generatedDocument);
            default -> generateTextFallbackContent(request, generatedDocument);
        };
    }

    private byte[] generatePdfContent(DocumentGenerationRequested request, DocumentGenerated generatedDocument) {
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Generated document</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            font-size: 14px;
                            margin: 32px;
                        }
                        h1 {
                            font-size: 22px;
                            margin-bottom: 16px;
                        }
                        .meta-row {
                            margin-bottom: 8px;
                        }
                        .label {
                            font-weight: bold;
                        }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <div class="meta-row"><span class="label">Document ID:</span> %s</div>
                    <div class="meta-row"><span class="label">Application ID:</span> %s</div>
                    <div class="meta-row"><span class="label">Request ID:</span> %s</div>
                    <div class="meta-row"><span class="label">Template:</span> %s:%s</div>
                    <div class="meta-row"><span class="label">Requested by:</span> %s</div>
                    <div class="meta-row"><span class="label">Generated at:</span> %s</div>
                    <div class="meta-row"><span class="label">Storage key:</span> %s</div>
                </body>
                </html>
                """.formatted(
                escapeHtml(request.documentType()),
                escapeHtml(generatedDocument.documentId().toString()),
                escapeHtml(request.applicationId().toString()),
                escapeHtml(request.requestId().toString()),
                escapeHtml(request.templateCode()),
                escapeHtml(request.templateVersion()),
                escapeHtml(request.requestedByUserId()),
                escapeHtml(generatedDocument.generatedAt().toString()),
                escapeHtml(generatedDocument.storageKey())
        );

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate PDF document", exception);
        }
    }

    private byte[] generateXmlContent(DocumentGenerationRequested request, DocumentGenerated generatedDocument) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <generatedDocument>
                    <documentId>%s</documentId>
                    <applicationId>%s</applicationId>
                    <requestId>%s</requestId>
                    <documentType>%s</documentType>
                    <templateCode>%s</templateCode>
                    <templateVersion>%s</templateVersion>
                    <requestedByUserId>%s</requestedByUserId>
                    <generatedAt>%s</generatedAt>
                    <storageKey>%s</storageKey>
                    <status>%s</status>
                </generatedDocument>
                """.formatted(
                escapeXml(generatedDocument.documentId().toString()),
                escapeXml(request.applicationId().toString()),
                escapeXml(request.requestId().toString()),
                escapeXml(request.documentType()),
                escapeXml(request.templateCode()),
                escapeXml(request.templateVersion()),
                escapeXml(request.requestedByUserId()),
                escapeXml(generatedDocument.generatedAt().toString()),
                escapeXml(generatedDocument.storageKey()),
                escapeXml(generatedDocument.status())
        );

        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateTextFallbackContent(DocumentGenerationRequested request, DocumentGenerated generatedDocument) {
        String content = """
                documentId=%s
                applicationId=%s
                requestId=%s
                documentType=%s
                templateCode=%s
                templateVersion=%s
                requestedByUserId=%s
                generatedAt=%s
                storageKey=%s
                status=%s
                """.formatted(
                generatedDocument.documentId(),
                request.applicationId(),
                request.requestId(),
                request.documentType(),
                request.templateCode(),
                request.templateVersion(),
                request.requestedByUserId(),
                generatedDocument.generatedAt(),
                generatedDocument.storageKey(),
                generatedDocument.status()
        );

        return content.getBytes(StandardCharsets.UTF_8);
    }

    private String resolveFileExtension(String format) {
        return switch (format) {
            case "PDF" -> "pdf";
            case "XML" -> "xml";
            default -> "bin";
        };
    }

    private String resolveMimeType(String format) {
        return switch (format) {
            case "PDF" -> "application/pdf";
            case "XML" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}