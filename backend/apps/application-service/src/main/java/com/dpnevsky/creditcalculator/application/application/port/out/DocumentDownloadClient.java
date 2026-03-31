package com.dpnevsky.creditcalculator.application.application.port.out;

import java.util.UUID;

public interface DocumentDownloadClient {

    DownloadedDocument downloadByDocumentId(UUID documentId);

    record DownloadedDocument(
            UUID documentId,
            String fileName,
            String mimeType,
            byte[] content
    ) {
    }
}