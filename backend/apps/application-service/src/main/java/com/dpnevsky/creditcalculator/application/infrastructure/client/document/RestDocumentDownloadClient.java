package com.dpnevsky.creditcalculator.application.infrastructure.client.document;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentDownloadClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class RestDocumentDownloadClient implements DocumentDownloadClient {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public RestDocumentDownloadClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.document.base-url}") String documentBaseUrl,
            @Value("${integration.document.internal-api-key}") String internalApiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(documentBaseUrl)
                .build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public DownloadedDocument downloadByDocumentId(UUID documentId) {
        String bearerToken = resolveBearerToken();

        ResponseEntity<byte[]> response = restClient.get()
                .uri("/internal/documents/{documentId}/download", documentId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + bearerToken)
                .header(INTERNAL_API_KEY_HEADER, internalApiKey)
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

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getTokenValue();
        }

        throw new IllegalStateException("Missing authenticated bearer token for document download");
    }
}
