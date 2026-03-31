package com.dpnevsky.creditcalculator.application.infrastructure.client.document;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentDownloadClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class RestDocumentDownloadClient implements DocumentDownloadClient {

    private static final String DEBUG_AUTH_HEADER = "X-Debug-Auth";
    private static final String DEBUG_AUTH_VALUE = "allow";

    private final RestClient restClient;

    public RestDocumentDownloadClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.document.base-url}") String documentBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(documentBaseUrl)
                .build();
    }

    @Override
    public DownloadedDocument downloadByDocumentId(UUID documentId) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri("/internal/documents/{documentId}/download", documentId)
                .header(DEBUG_AUTH_HEADER, DEBUG_AUTH_VALUE)
                .retrieve()
                .toEntity(byte[].class);

        byte[] content = response.getBody();
        if (content == null) {
            throw new IllegalStateException(
                    "Downloaded document content is empty. documentId=" + documentId
            );
        }

        String mimeType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        String fileName = resolveFileName(response.getHeaders(), documentId);

        return new DownloadedDocument(
                documentId,
                fileName,
                mimeType,
                content
        );
    }

    private String resolveFileName(HttpHeaders headers, UUID documentId) {
        String contentDispositionHeader = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDispositionHeader == null || contentDispositionHeader.isBlank()) {
            return documentId + ".bin";
        }

        ContentDisposition contentDisposition = ContentDisposition.parse(contentDispositionHeader);
        String fileName = contentDisposition.getFilename();

        if (fileName == null || fileName.isBlank()) {
            return documentId + ".bin";
        }

        return fileName;
    }
}