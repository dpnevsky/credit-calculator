# Filesystem Storage

Document-service stores generated documents on the local filesystem.

## Local development

Default storage path: `./document-storage/` (relative to service working directory).

Override via `document.storage.base-path` application property.

## Kubernetes

In Kubernetes, mount a PersistentVolumeClaim to the document-service pod
at `/app/document-storage` or use an S3-compatible object store (Stage 2).
