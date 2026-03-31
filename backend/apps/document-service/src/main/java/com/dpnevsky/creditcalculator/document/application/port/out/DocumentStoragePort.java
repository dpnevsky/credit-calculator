package com.dpnevsky.creditcalculator.document.application.port.out;

public interface DocumentStoragePort {

    void save(SaveDocumentCommand command);

    StoredDocument load(String storageKey);

    record SaveDocumentCommand(
            String storageKey,
            byte[] content
    ) {
    }

    record StoredDocument(
            byte[] content
    ) {
    }
}

