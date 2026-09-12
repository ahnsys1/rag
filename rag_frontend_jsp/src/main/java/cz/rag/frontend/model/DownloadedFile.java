package cz.rag.frontend.model;

/** Raw file streamed back from the backend download endpoint. */
public record DownloadedFile(String contentType, String contentDisposition, byte[] data) { }
